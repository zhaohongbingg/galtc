package com.galtc2.modifiers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

/**
 * 零八式·乱射（{@code galtc2:eightstrike_ranged}）—— zeroone 材料的<b>远程</b>特性：
 * 一次射击打出 <b>{@value #ARROW_COUNT} 支箭</b>，只消耗 1 支弹药。
 * <p>
 * <b>为什么不用 multishot 数据/词条</b>：弓实射箭数来自 {@code BowAmmoModifierHook} 里
 * {@code projectilesDesired = 1 + 2 * multishotLevel}，只会是 <b>1/3/5/7/9</b> 支，拿不到 8 支；
 * 且耐久按箭数扣。这里自己做"补射"，并把每支箭的伤害按 {@link #DAMAGE_SCALE} 下调，
 * 让 8 支箭的总伤害 ≈ 1 支（避免 8 倍 DPS），代价是"8 次独立命中判定"。
 * <p>
 * 主箭之外的 {@value #EXTRA_ARROW_COUNT} 支：
 * <ul>
 *   <li>方向照匠魂 {@code ModifiableLauncherItem} 的多重射击角度：起始角 {@code -5*(N-1)}，每支 +10°；</li>
 *   <li>复制主箭口径：{@code baseDamage × DAMAGE_SCALE}、同一份 modifier 列表（否则补射的箭不触发任何特性）、
 *       同一份 persistent data、{@code pickup = CREATIVE_ONLY}（避免刷箭）；</li>
 *   <li>只对玩家生效（女仆/生物射箭不走这套，避免 AI 一次打出 8 支把服务器打爆）。</li>
 * </ul>
 * <b>防重入</b>：{@code PROJECTILE_LAUNCH} 是"每支箭一次"，主箭 + 补射箭都会回调；
 * 用弓自己 persistent data 里的 gameTime 闸门（{@value #GATE_TICKS} tick）+ {@code primary} 双重判定。
 * <p>
 * 本类<b>不注册</b>（注册与 lang 由 b-register 负责），类里只给 id 常量。
 */
public class EightStrikeRangedModifier extends Modifier implements ProjectileLaunchModifierHook {

    /** 词条 id（注册时用同一个字符串） */
    public static final String ID = "eightstrike_ranged";

    /** 一次射击的总箭数 */
    public static final int ARROW_COUNT = 8;
    /** 补射的箭数 = 总数 - 主箭 */
    private static final int EXTRA_ARROW_COUNT = ARROW_COUNT - 1;
    /**
     * 每支箭的伤害系数。用户口径（2026-10-27）：<b>视觉 8 支、伤害也是 8 支</b>、只消耗 1 支弹药，
     * 所以这里取 <b>1.0</b>（每支箭都吃满弓的伤害）。早先的 0.125「总伤≈1 支」是保守值，已按用户要求放平。
     * 想回调强度就改这一个常量。
     */
    public static final double DAMAGE_SCALE = 1.0D;
    /** 相邻两支箭的角度差（度），与匠魂 multishot 一致 */
    private static final float ANGLE_STEP = 10.0F;
    /** 重入闸门（tick）：同一把弓 2 tick 内只补射一次 */
    private static final long GATE_TICKS = 2L;

    /** 弓上的补射闸门（gameTime） */
    private static final ResourceLocation KEY_LAST_BURST = ResourceLocation.fromNamespaceAndPath("galtc2", "eightstrike_last_burst");

    public EightStrikeRangedModifier() {}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_LAUNCH);
    }

    /**
     * 旧 7 参签名（没有 ammo）：接口里是抽象方法，必须实现。
     * 真正的补射逻辑写在 8 参重载里 —— {@code ProjectileLaunchModifierHook.AllMerger} 调的是 8 参版本。
     */
    @Override
    @Deprecated
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
                                   Projectile projectile, @Nullable AbstractArrow arrow,
                                   ModDataNBT persistentData, boolean primary) {
    }

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
                                   ItemStack ammo, Projectile projectile, @Nullable AbstractArrow arrow,
                                   ModDataNBT persistentData, boolean primary) {
        if (!primary || arrow == null) return;
        if (shooter == null || shooter.level().isClientSide()) return;
        // 只对玩家生效：避免女仆/骷髅一次 8 支
        if (!(shooter instanceof net.minecraft.world.entity.player.Player)) return;
        if (ammo.isEmpty() || !(ammo.getItem() instanceof ArrowItem arrowItem)) return;

        // 闸门：multishot 之类一次射击会多次回调，2 tick 内只补一次
        ModDataNBT toolData = tool.getPersistentData();
        long now = shooter.level().getGameTime();
        long last = toolData.contains(KEY_LAST_BURST) ? toolData.get(KEY_LAST_BURST, CompoundTag::getLong) : 0L;
        if (now - last < GATE_TICKS) return;
        toolData.put(KEY_LAST_BURST, LongTag.valueOf(now));

        double baseDamage = arrow.getBaseDamage() * DAMAGE_SCALE;
        // 匠魂的多重射击起始角：-5*(N-1)，每支 +10°（角度总量 -35°…+35° 不变）
        float startAngle = ModifiableLauncherItem.getAngleStart(ARROW_COUNT);

        for (int i = 0; i < EXTRA_ARROW_COUNT; i++) {
            AbstractArrow extra = arrowItem.createArrow(shooter.level(), ammo, shooter);
            if (extra == null) continue;

            extra.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
            float angle = startAngle + ANGLE_STEP * i;
            // 水平扇形（与 vanilla multishot 一致）：角度加在 **yaw** 上，pitch 保持射手的视线俯仰。
            // 用户 2026-10-27 反馈：之前加在 pitch 上是上下铺开，竖向不对，应该是左右铺开。
            extra.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle, 0.0F,
                    (float) arrow.getDeltaMovement().length(), 0.0F);

            // 复制主箭口径：伤害、modifier 列表、persistent data、拾取方式
            extra.setBaseDamage(baseDamage);
            extra.setCritArrow(arrow.isCritArrow());
            extra.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            extra.getCapability(EntityModifierCapability.CAPABILITY)
                    .ifPresent(cap -> cap.setModifiers(tool.getModifiers()));
            extra.getCapability(PersistentDataCapability.CAPABILITY)
                    .ifPresent(data -> data.copyFrom(persistentData.getCopy()));

            shooter.level().addFreshEntity(extra);
        }
    }
}
