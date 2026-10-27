package com.galtc2.compat.relicward.modifiers;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.combat.ArmorFracture;
import cn.suiyi.relicward.combat.CombatMath;
import cn.suiyi.relicward.combat.PlayerCombat;
import cn.suiyi.relicward.entity.MaulPulse;
import com.galtc2.Galtc2;
import com.galtc2.modifiers.TraitLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * 回响震击 (echo_slam) —— 遗境守望 联动攻击特性，挂在 钟铸铜板 材料上。
 * <p>
 * <b>右键蓄力，松手落锤</b>：
 * <ul>
 *   <li>伤害 = <b>面板伤害 × 蓄力秒数 × 特性等级</b>
 *       —— 面板伤害取玩家当前的 {@link Attributes#ATTACK_DAMAGE}
 *       （也就是物品栏里"攻击伤害"显示的那个数，已经把手里这把匠魂工具的加成算进去了）；
 *       蓄力按 tick 计、除以 20 折成秒；等级口径见 {@link TraitLevels#weaponLevel}</li>
 *   <li>最长蓄力 {@value #MAX_CHARGE} tick（{@value #MAX_CHARGE} / 20 = 9 秒），
 *       因为伤害会随蓄力线性成长，不封顶的话按着右键不放就能砸出天文数字；
 *       蓄满这个上限会<b>自动落锤</b>（原版物品用尽 → {@code onFinishUsing}），不用自己松手；
 *       松手不足 {@value #MIN_CHARGE} tick（0.5 秒）视为没蓄力，不触发（照 JVT 蓄力施法强化）</li>
 *   <li>蓄力过程中用 {@link UseAnim#BOW} 做个"举起来"的动作 —— 模组本体（回响钟锤）
 *       的 {@code getUseAnimation} 是 {@code UseAnim.NONE}，但这里蓄力最长 9 秒，
 *       不给动作玩家看不出自己在蓄力</li>
 * </ul>
 * <b>蓄力提示</b>（两条，都在客户端看得见）：
 * <ul>
 *   <li>蓄力期间每 {@value #CHARGE_HINT_INTERVAL} tick 在快捷栏上方刷一行
 *       "蓄力 x.x / 9.0 秒 · 伤害 ×N（M 级）"
 *       —— 秒数是当前蓄力时间，倍率已经把等级乘进去了（{@code 秒数 × 等级}）；
 *       只在服务端发（{@link Player#displayClientMessage}），免得两边各发一次</li>
 *   <li>工具提示里常驻一行上限提示（{@link TooltipModifierHook}），语言键
 *       {@code modifier.galtc2.echo_slam.cap}</li>
 * </ul>
 * <b>冷却按"这件工具"各自算</b>（{@value #COOLDOWN} tick = 5 秒）：
 * 剩余时间写在<b>工具自己的持久数据</b>里（{@link IToolStackView#getPersistentData()}），
 * 服务端在背包 tick 里递减，客户端靠物品 NBT 同步读到进度。
 * <p>
 * 为什么不用原版那两种冷却：
 * <ul>
 *   <li>原版 {@code ItemCooldowns} 是<b>按物品类型</b>记的，给匠魂工具刷冷却会让
 *       "一把剑进冷却 = 所有剑一起进冷却"；</li>
 *   <li>遗境守望 自己的账本（{@link cn.suiyi.relicward.item.RelicEquipment}）是<b>按玩家</b>记的，
 *       本来就是为了"换个武器也逃不掉同一个招式的冷却"，用它同样会牵连别的工具
 *       （而且会牵连模组本体的回响钟锤）。</li>
 * </ul>
 * 现在换成按工具记：每一把工具各攒各的冷却，同时手里那件真正的回响钟锤也走模组自己那套，互不影响。
 * <p>
 * <b>落锤本身完全照回响钟锤的震击</b>（{@code EchoMaulItem.slam}）：
 * <ul>
 *   <li>落点用复刻的 {@link RelicWardHelper#maulGroundPoint}（前移 1.8 格、往下探 5 格、
 *       取方块碰撞箱最高点）；前方是悬空就不出手，并照模组弹 {@code message.relicward.maul_ground}；</li>
 *   <li>范围：{@code inflate(5, 2, 5)} 粗筛 + 模组 {@link CombatMath#cone}（5 格 / 120 度）精筛，
 *       高差超过 {@value #Y_TOLERANCE} 格的不打；目标过滤交给模组 {@link PlayerCombat#canHit}；</li>
 *   <li>命中附 {@link ArmorFracture#apply(LivingEntity, int)}（{@value #FRACTURE_TICKS} tick = 5 秒裂甲）
 *       与 {@link LivingEntity#knockback(double, double, double)}（{@value #KNOCKBACK}，方向照模组写法）；</li>
 *   <li>表现：模组的 {@link MaulPulse} 特效实体 + 22 颗 {@link ParticleTypes#CRIT} +
 *       {@link SoundEvents#ANVIL_LAND}（0.7 / 0.65）+ 模组台词 {@code message.relicward.maul_slam}。</li>
 * </ul>
 */
public class EchoSlamModifier extends Modifier
        implements GeneralInteractionModifierHook, TooltipModifierHook, InventoryTickModifierHook,
        ProjectileHitModifierHook {

    /** 最长蓄力（tick）：9 秒 */
    private static final int MAX_CHARGE = 180;
    /** 最短蓄力（tick）：低于这个时长松手不触发 */
    private static final int MIN_CHARGE = 10;
    /** 冷却（tick）：5 秒，按工具各算各的 */
    private static final int COOLDOWN = 100;
    /** 蓄力提示的刷新间隔（tick）：4 tick 一次，够跟手又不至于刷屏 */
    private static final int CHARGE_HINT_INTERVAL = 4;
    /** 锥形范围：前方格数 / 张角（度），与回响钟锤一致 */
    private static final double REACH = 5.0;
    private static final double CONE_ANGLE = 120.0;
    /** 纵向容差：与玩家脚底高差超过这个数的目标不参与判定 */
    private static final double Y_TOLERANCE = 2.5;
    /** 命中后的裂甲时长（tick）：5 秒 */
    private static final int FRACTURE_TICKS = 100;
    /** 击退力度（与回响钟锤震击一致） */
    private static final double KNOCKBACK = 0.7;
    /** 冷却剩余 tick 存在工具自己的持久数据里：每把工具各算各的 */
    private static final ResourceLocation KEY_COOLDOWN = Galtc2.loc("echo_slam_cooldown");

    public EchoSlamModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.TOOLTIP, ModifierHooks.INVENTORY_TICK,
                ModifierHooks.PROJECTILE_HIT);
    }

    /**
     * 远程：箭矢命中时把「震击」的两条效果打上去 —— <b>裂甲 5 秒 + 0.7 击退</b>，
     * 与右键落锤命中同一套数值/写法（照 {@code ArmorFractureModifier#onProjectileHitTarget}）。
     * <p>箭矢本身伤害由原版结算，这里不调 {@code target.hurt()}，只补效果与击退。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null || !target.isAlive()) {
            return false;
        }
        if (projectile.level().isClientSide() || modifier.getEffectiveLevel() <= 0) {
            return false;
        }
        ArmorFracture.apply(target, FRACTURE_TICKS);
        target.knockback(KNOCKBACK, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        return false;
    }

    /** 工具提示里的上限提示：蓄力最多 [{@value #MAX_CHARGE} / 20] 秒，到点自动落锤 */
    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player,
                           List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable("modifier.galtc2.echo_slam.cap", MAX_CHARGE / 20)
                .withStyle(ChatFormatting.GRAY));
    }

    /**
     * 冷却递减：每 tick 减 1，只在服务端写（客户端读同步过来的值就行，
     * 两边各自递减会把数字减成两倍速）。
     */
    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder,
                                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (level.isClientSide()) {
            return;
        }
        int cooldown = tool.getPersistentData().getInt(KEY_COOLDOWN);
        if (cooldown > 0) {
            tool.getPersistentData().putInt(KEY_COOLDOWN, cooldown - 1);
        }
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player,
                                       InteractionHand hand, InteractionSource source) {
        if (source != InteractionSource.RIGHT_CLICK || tool.isBroken()) {
            return InteractionResult.PASS;
        }
        // 冷却中不给起手：读的是这件工具自己的持久数据，别的工具不受影响
        if (isOnCooldown(tool)) {
            return InteractionResult.PASS;
        }
        GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return MAX_CHARGE;
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAnim.BOW;
    }

    /** 蓄力进度提示：当前蓄力秒数 / 上限，以及乘上等级后的伤害倍率 */
    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player) || user.level().isClientSide()) {
            return;
        }
        int charge = MAX_CHARGE - timeLeft;
        if (charge <= 0 || charge % CHARGE_HINT_INTERVAL != 0) {
            return;
        }
        int traitLevel = Math.max(1, TraitLevels.weaponLevel(player, modifier.getId()));
        float seconds = charge / 20.0f;
        player.displayClientMessage(Component.translatable("modifier.galtc2.echo_slam.charge",
                format(seconds), format(MAX_CHARGE / 20.0f), format(seconds * traitLevel), traitLevel), true);
    }

    /** 蓄满自动出手（原版物品用尽） */
    @Override
    public void onFinishUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity) {
        slam(tool, modifier, entity, MAX_CHARGE);
    }

    /** 中途松手 */
    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        slam(tool, modifier, entity, MAX_CHARGE - timeLeft);
    }

    /** 蓄力结束落锤；蓄力不足、冷却中、前方没地面都不出手 */
    private static void slam(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int chargeTicks) {
        if (chargeTicks < MIN_CHARGE || !(entity instanceof Player player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (isOnCooldown(tool)) {
            return;
        }
        int traitLevel = TraitLevels.weaponLevel(player, modifier.getId());
        if (traitLevel <= 0) {
            return;
        }
        Vec3 ground = RelicWardHelper.maulGroundPoint(level, player);
        if (ground == null) {
            player.displayClientMessage(Component.translatable("message.relicward.maul_ground"), true);
            return;
        }

        // 伤害 = 面板伤害 × 蓄力秒数 × 等级
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (chargeTicks / 20.0f) * traitLevel;

        // 冷却记在这件工具自己身上
        tool.getPersistentData().putInt(KEY_COOLDOWN, COOLDOWN);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(REACH, 2.0, REACH))) {
            if (!PlayerCombat.canHit(player, target)) {
                continue;
            }
            if (Math.abs(target.getY() - player.getY()) > Y_TOLERANCE) {
                continue;
            }
            if (!CombatMath.cone(player.position(), player.getYRot(), target.position(), REACH, CONE_ANGLE)) {
                continue;
            }
            if (target.hurt(level.damageSources().playerAttack(player), damage)) {
                ArmorFracture.apply(target, FRACTURE_TICKS);
                target.knockback(KNOCKBACK, player.getX() - target.getX(), player.getZ() - target.getZ());
            }
        }

        MaulPulse pulse = RelicWard.MAUL_PULSE.get().create(level);
        if (pulse != null) {
            pulse.moveTo(ground.x, ground.y + 0.01, ground.z, player.getYRot(), 0.0f);
            level.addFreshEntity(pulse);
        }
        level.sendParticles(ParticleTypes.CRIT, ground.x, ground.y + 0.15, ground.z, 22, 1.0, 0.1, 1.0, 0.15);
        level.playSound(null, BlockPos.containing(ground), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 0.65f);
        player.displayClientMessage(Component.translatable("message.relicward.maul_slam"), true);
    }

    /** 这件工具自己还在冷却里吗 */
    private static boolean isOnCooldown(IToolStackView tool) {
        return tool.getPersistentData().getInt(KEY_COOLDOWN) > 0;
    }

    /** 秒数保留一位小数（提示文案用） */
    private static String format(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
