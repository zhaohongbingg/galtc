package com.galtc2.compat.meetyourfight.modifiers;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 舞亚 (rose_wraith) —— 迎战 联动特性：暮光紫罗兰 材料的防御侧特性。
 * <p>
 * 被攻击时，在攻击者旁边召唤一只迎战的<b>蔷薇怨灵</b>替你反击。
 * <p>
 * <b>怎么让它"认玩家为主"</b>（迎战 那边的路都被堵死了，只能这样绕）：
 * <ul>
 *   <li>迎战 {@code RoseSpiritEntity#setOwner} 只收 {@code Mob}，玩家不是 {@code Mob}，
 *       所以没法把玩家塞进去；那些"召唤友方恼鬼"的 mod（如 Ars Nouveau）能这么做，
 *       是因为它们<b>自己写了实体类</b>（{@code AllyVex extends Vex} + 自己注册 goal + LivingEntity 主人），
 *       在别人现成的实体上做不到，除非连模型贴图一起自研（迎战 的资源是 ARR，不能复用）。</li>
 *   <li>于是这里给它配一个 <b>隐形 / 无 AI / 无敌 / 静音</b>的 {@link Vex} 当"锚点主人"：
 *       锚点每 tick 跟着玩家走，目标设为玩家的敌人，怨灵就用迎战自己的
 *       {@code CopyOwnerTargetGoal}（继承主人目标）和 {@code MoveAroundOwner}（绕着主人飞）干活 ——
 *       表现就是"跟在玩家身边、打玩家的敌人"。</li>
 *   <li>再加一层保险：每 tick 把怨灵的目标也直接钉到玩家的敌人上（怨灵自己的
 *       {@code NearestAttackableTargetGoal<Player>} 想索敌玩家时会被立刻覆盖，它开火有 10+ tick 前摇）。
 *       玩家本来就有计分板队伍时，顺便把怨灵拉进同队 —— 原版 {@code TargetingConditions.test}
 *       里有 {@code pAttacker.isAlliedTo(pTarget)} 判定，同队即同盟，AI 层面就不会打玩家了。</li>
 *   <li>兜底回收：15 秒寿命、敌人没了、玩家死了都会 {@code discard()}（怨灵和锚点一起清），
 *       不然潜行无敌的怨灵会残留成打不掉的野怪。</li>
 * </ul>
 * 实体类型用 {@link ForgeRegistries} 按 ID 查，不直接引用迎战的类。
 */
public class RoseWraithModifier extends Modifier implements OnAttackedModifierHook {

    /** 触发概率 */
    private static final float CHANCE = 0.25f;
    /** 召唤冷却（10 秒） */
    private static final int COOLDOWN = 10 * 20;
    /** 怨灵存活时间（15 秒） */
    private static final int LIFETIME = 15 * 20;
    /** 锚点的隐身时长（只要比怨灵寿命长就行） */
    private static final int ANCHOR_INVISIBLE_TICKS = 60 * 20;
    /** 迎战 的蔷薇怨灵实体 ID */
    private static final ResourceLocation ROSE_SPIRIT_ID = new ResourceLocation("meetyourfight", "rose_spirit");

    /** 玩家 -> 上次召唤时间 */
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    /** 已召唤的怨灵 */
    private static final List<Summon> SUMMONS = new ArrayList<>();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean eventsRegistered = false;
    /** setOwner 反射失败只提示一次 */
    private static boolean ownerCallFailedLogged = false;

    /** 一只已召唤的怨灵 + 它的锚点主人 + 归属信息 */
    private static final class Summon {
        private final Mob spirit;
        /** 隐形锚点：怨灵的 owner，同时也是它"跟着谁飞"的参照物 */
        private final Mob anchor;
        private final LivingEntity summoner;
        private LivingEntity enemy;
        private final long expireAt;

        private Summon(Mob spirit, Mob anchor, LivingEntity summoner, LivingEntity enemy, long expireAt) {
            this.spirit = spirit;
            this.anchor = anchor;
            this.summoner = summoner;
            this.enemy = enemy;
            this.expireAt = expireAt;
        }
    }

    public RoseWraithModifier() {
    }

    /** 只注册一次服务端 tick 清理/维护器（由 {@code MeetYourFightCompat#init} 调用）。 */
    public static void registerEvents() {
        if (eventsRegistered) {
            return;
        }
        eventsRegistered = true;
        MinecraftForge.EVENT_BUS.addListener(RoseWraithModifier::onServerTick);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                           EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity owner = context.getEntity();
        if (owner == null || owner.level().isClientSide()) {
            return;
        }
        if (slotType != MeetYourFightHelper.firstArmorSlot(context, this)) {
            return;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == owner || !attacker.isAlive()) {
            return;
        }
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }

        long now = level.getGameTime();
        Long last = COOLDOWNS.get(owner.getUUID());
        if (last != null && now - last < COOLDOWN) {
            return;
        }
        if (RANDOM.nextFloat() >= CHANCE) {
            return;
        }
        if (summon(level, owner, attacker, now)) {
            COOLDOWNS.put(owner.getUUID(), now);
        }
    }

    /** 召唤怨灵 + 锚点，并把怨灵交给锚点"认主"。 */
    private static boolean summon(ServerLevel level, LivingEntity summoner, LivingEntity attacker, long now) {
        EntityType<?> spiritType = ForgeRegistries.ENTITY_TYPES.getValue(ROSE_SPIRIT_ID);
        if (spiritType == null) {
            return false;
        }
        Entity created = spiritType.create(level);
        if (!(created instanceof Mob spirit)) {
            return false;
        }
        Mob anchor = createAnchor(level, summoner);
        if (anchor == null) {
            spirit.discard();
            return false;
        }

        double x = attacker.getX() + (RANDOM.nextDouble() - 0.5) * 3.0;
        double z = attacker.getZ() + (RANDOM.nextDouble() - 0.5) * 3.0;
        spirit.moveTo(x, attacker.getY() + 1.0, z, RANDOM.nextFloat() * 360.0f, 0.0f);
        // 认主：锚点跟着玩家走 + 目标 = 玩家的敌人，怨灵自己的 AI 就会照做
        trySetOwner(spirit, anchor);
        spirit.setTarget(attacker);
        anchor.setTarget(attacker);
        level.addFreshEntity(spirit);

        // 玩家本来就有计分板队伍时，把怨灵拉进同队：原版 TargetingConditions 会因此把它视为同盟，
        // AI 层面就不会把玩家当目标了（玩家没队伍就不动，免得影响玩家自身表现）。
        Team team = summoner.getTeam();
        if (team instanceof PlayerTeam playerTeam) {
            level.getScoreboard().addPlayerToTeam(spirit.getStringUUID(), playerTeam);
        }

        SUMMONS.add(new Summon(spirit, anchor, summoner, attacker, now + LIFETIME));
        return true;
    }

    /**
     * 造一只隐形、无 AI、无敌、静音的恼鬼当锚点。
     * 它只是"怨灵的 owner + 跟随参照"，不参与战斗，也会跟着怨灵一起被清掉。
     */
    private static Mob createAnchor(ServerLevel level, LivingEntity summoner) {
        Vex anchor = EntityType.VEX.create(level);
        if (anchor == null) {
            return null;
        }
        anchor.moveTo(summoner.getX(), summoner.getY() + 1.0, summoner.getZ(), 0.0f, 0.0f);
        anchor.setNoAi(true);
        anchor.setSilent(true);
        anchor.setInvulnerable(true);
        anchor.setNoGravity(true);
        anchor.setPersistenceRequired();
        anchor.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ANCHOR_INVISIBLE_TICKS, 0, false, false));
        level.addFreshEntity(anchor);
        return anchor;
    }

    /**
     * 调迎战 怨灵的 {@code setOwner(Mob)}。
     * <p>
     * 这里走反射而不是直接 import 迎战的实体类：一是签名/类名变了不会把联动编译搞崩，
     * 二是万一以后没这个 API，也只是退化成"只钉目标"模式（下面的 retarget 仍然管用）。
     */
    private static void trySetOwner(Mob spirit, Mob owner) {
        try {
            spirit.getClass().getMethod("setOwner", Mob.class).invoke(spirit, owner);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!ownerCallFailedLogged) {
                ownerCallFailedLogged = true;
                LOGGER.info("[galtc2] 迎战 蔷薇怨灵不支持 setOwner(Mob)，舞亚退化为只钉目标模式：{}", e.toString());
            }
        }
    }

    /** 服务端每 tick：锚点跟人、目标钉在玩家的敌人上，到点/没敌人/玩家死了就一起回收。 */
    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (SUMMONS.isEmpty() && COOLDOWNS.isEmpty()) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();

        Iterator<Summon> iterator = SUMMONS.iterator();
        while (iterator.hasNext()) {
            Summon summon = iterator.next();
            boolean done = summon.spirit.isRemoved() || !summon.spirit.isAlive()
                    || summon.summoner.isRemoved() || !summon.summoner.isAlive()
                    || now >= summon.expireAt
                    // 目标没了就换玩家最近打的怪，再没有就回收
                    || !retarget(summon);
            if (done) {
                discard(summon);
                iterator.remove();
            }
        }

        COOLDOWNS.entrySet().removeIf(entry -> now - entry.getValue() > COOLDOWN);
    }

    /** 锚点跟人 + 保证目标一直是玩家这边的敌人；实在没有敌人才返回 false（→ 回收）。 */
    private static boolean retarget(Summon summon) {
        if (summon.enemy == null || !summon.enemy.isAlive() || summon.enemy.isRemoved()) {
            LivingEntity next = summon.summoner.getLastHurtMob();
            if (next != null && next != summon.summoner && next.isAlive()) {
                summon.enemy = next;
            } else {
                return false;
            }
        }
        // 锚点跟着玩家（怨灵靠 MoveAroundOwner 绕着锚点飞，所以也就是绕着你飞）
        summon.anchor.moveTo(summon.summoner.getX(), summon.summoner.getY() + 1.0, summon.summoner.getZ(),
                summon.anchor.getYRot(), 0.0f);
        if (summon.anchor.getTarget() != summon.enemy) {
            summon.anchor.setTarget(summon.enemy);
        }
        // 双保险：直接把怨灵的目标也钉上（它自己的索敌想抢也会被下一 tick 覆盖）
        if (summon.spirit.getTarget() != summon.enemy) {
            summon.spirit.setTarget(summon.enemy);
        }
        return true;
    }

    /** 怨灵和锚点一起清掉（怨灵潜行时无敌、锚点又设了 persistence，必须自己回收）。 */
    private static void discard(Summon summon) {
        if (!summon.spirit.isRemoved()) {
            summon.spirit.discard();
        }
        if (!summon.anchor.isRemoved()) {
            summon.anchor.discard();
        }
    }
}
