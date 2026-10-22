package com.galtc2.compat.bomd.modifiers;

import com.cerbon.bosses_of_mass_destruction.entity.BMDEntities;
import com.cerbon.bosses_of_mass_destruction.util.BMDUtils;
import com.cerbon.cerbons_api.api.general.event.EventScheduler;
import com.cerbon.cerbons_api.api.static_utilities.CapabilityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;

/**
 * 祸乱鬼魅(BOMD) 联动特性共用的小工具。
 * <p>
 * <b>本类会引用 BOMD 的类型，只能在确认 BOMD 已安装之后加载</b>
 * —— 也就是只能被 {@code BomdModifiers} 及其下面的强化类引用，
 * 而 {@code BomdModifiers} 又只在 {@link com.galtc2.compat.bomd.BomdCompat#init} 判断通过后才会被触碰。
 * <p>
 * 护甲钩子（{@code MODIFY_HURT}）对「每件带该特性的护甲」各调一次，
 * 所以减伤这类效果要先确定"由哪一件护甲负责结算"，否则穿 4 件就会结算 4 次。
 * 这里按固定槽位顺序（脚→腿→胸→头）取第一件。
 */
public final class BomdHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("galtc2/bomd");

    /**
     * 联动技能专用伤害类型（{@code data/galtc2/damage_type/linkage.json}）。
     * <p>
     * 比 BOMD 的 {@code bosses_of_mass_destruction:shield_piercing} 多一个
     * {@code "bypasses_cooldown": true}，这是必须的 —— 这些效果全都撞在无敌帧上：
     * <ul>
     *   <li><b>地刺</b>是「先预警、延迟 20 tick 再结算」，而玩家的挥砍会给目标刷上 20 tick 无敌帧，
     *       刺的伤害（{@code ATTACK_DAMAGE} 属性值）又永远不高于你实际砍出来的伤害 ——
     *       于是 {@code LivingEntity.hurt} 每次都走「伤害不够高就直接 return false」，一次都打不进去；</li>
     *   <li><b>激光</b>每 2 tick 结算一次，远快于 20 tick 的无敌帧，不去掉的话整段持续射击
     *       只会生效第一下（表现就是"伤害恒定 1.5"）。</li>
     * </ul>
     */
    private static final ResourceKey<DamageType> LINKAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("galtc2", "linkage"));

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private BomdHelper() {
    }

    /**
     * 是不是祸乱鬼魅的 Boss「暗夜巫妖」。
     * <p>
     * 直接拿注册表里的实例比较，比按 id 字符串比对更省事，也不会被改名影响。
     */
    public static boolean isNightLich(@Nullable Entity entity) {
        return entity != null && entity.getType() == BMDEntities.LICH.get();
    }

    /** 穿戴了该强化的第一件护甲槽位；一件都没有时返回 null */
    @Nullable
    public static EquipmentSlot firstArmorSlot(EquipmentContext context, Modifier modifier) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(modifier) > 0) {
                return slot;
            }
        }
        return null;
    }

    /**
     * 找实体脚下"可以放东西"的那一格 —— 即实体所在位置往下第一个固体方块的正上方。
     * <p>
     * 找不到（悬空超过 4 格 / 掉到世界底部）时返回 null，调用方据此跳过本次生成。
     */
    @Nullable
    public static BlockPos findGroundPos(Level level, Entity entity) {
        BlockPos.MutableBlockPos cursor = entity.blockPosition().mutable();
        for (int i = 0; i < 4 && cursor.getY() > level.getMinBuildHeight(); i++) {
            BlockPos below = cursor.below();
            if (level.getBlockState(below).isSolidRender(level, below)) {
                return cursor.immutable();
            }
            cursor.move(0, -1, 0);
        }
        return null;
    }

    /**
     * 在实体脚下附近随机挑一格可以放方块的位置，用来做「随机生成地刺」这类效果。
     *
     * @param radius 水平方向的随机半径（格）
     */
    @Nullable
    public static BlockPos randomGroundPosNear(Level level, Entity entity, int radius, RandomSource random) {
        BlockPos base = findGroundPos(level, entity);
        if (base == null) {
            return null;
        }
        int dx = random.nextInt(radius * 2 + 1) - radius;
        int dz = random.nextInt(radius * 2 + 1) - radius;
        BlockPos candidate = base.offset(dx, 0, dz);
        // 只有目标格是空的、且下方有实体方块时才落点，避免悬在半空
        if (!level.getBlockState(candidate).canBeReplaced()) {
            return null;
        }
        BlockPos belowCandidate = candidate.below();
        return level.getBlockState(belowCandidate).isSolidRender(level, belowCandidate) ? candidate : null;
    }

    /**
     * 取当前世界的事件调度器。
     * <p>
     * BOMD 的两种尖刺（{@code void_blossom.Spikes} / {@code obsidilith.RiftBurst}）自己不 tick，
     * 而是把「延迟一会儿再结算伤害」注册进 cerbons_api 挂在 Level 上的 {@link EventScheduler}。
     * 只有服务端能拿到；拿不到时返回 {@code null}，调用方跳过本次生成即可。
     */
    @Nullable
    public static EventScheduler levelScheduler(Level level) {
        return CapabilityUtils.getLevelEventScheduler(level);
    }

    /**
     * 照 BOMD 的 {@code RiftBurst.defaultPosFinder} 找一根刺该落在哪一格：
     * 先把输入点抬高 14 格，再往下找到第一个「上表面可站立」的方块，取它的正上方。
     * 太深（相差超过 28 格）或那一格不可替换时返回 {@code null}，调用方跳过该落点。
     */
    @Nullable
    public static BlockPos findSpikePos(Level level, Vec3 pos) {
        BlockPos above = BlockPos.containing(pos.add(0.0, 14.0, 0.0));
        BlockPos ground = BMDUtils.findGroundBelow(level, above, p -> Boolean.TRUE);
        BlockPos up = ground.above();
        if (up.getY() + 28 < above.getY()) {
            return null;
        }
        return level.getBlockState(up).canBeReplaced() ? up : null;
    }

    /**
     * 用联动伤害类型打一下（{@link #LINKAGE} 带 {@code bypasses_cooldown}，不会被无敌帧吃掉），
     * 返回是否真的生效。
     */
    public static boolean linkageHit(ServerLevel level, LivingEntity attacker, LivingEntity victim, float damage) {
        DamageSource source = new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(LINKAGE), attacker);
        boolean applied = victim.hurt(source, damage);
        LOGGER.info("[联动伤害] {} 承伤 {}（是否生效={}）", victim.getName().getString(), damage, applied);
        return applied;
    }

    /**
     * 地刺伤害 = 施法者攻击力面板 × 这个系数。
     * <p>
     * 用一半：地刺是"命中后额外补一下"，不该比你自己砍的那一刀还疼；
     * 激光（{@code BlazingEyeModifier}）用的是同一个口径。
     */
    private static final float DAMAGE_SCALE = 0.5f;

    /**
     * 结算一次地刺伤害：伤害值取施法者当前攻击力的一半（玩家身上已经算进了手里匠魂工具的加成）。
     */
    public static boolean thornHit(ServerLevel level, LivingEntity attacker, LivingEntity victim) {
        float damage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_SCALE;
        return linkageHit(level, attacker, victim, damage);
    }
}
