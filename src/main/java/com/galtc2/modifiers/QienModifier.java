package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

public class QienModifier extends Modifier implements MeleeDamageModifierHook, ProjectileHitModifierHook {

    public QienModifier() {}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if (context.getAttacker() == null || context.getAttacker().level().isClientSide()) {
            return damage;
        }

        return damage + clockBonus();
    }

    /**
     * 箭矢命中：按真实时间追加 {@code max(小时, 分钟)} 点伤害（与近战同一口径）。
     * <p>
     * 追加伤害走 {@code attackEntitySecondary}（不碰 {@code target.hurt}），来源挂箭矢，
     * 否则箭矢自身伤害会被无敌帧吞掉。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null) return false;
        if (attacker.level().isClientSide() || target.isDeadOrDying()) return false;
        if (!(projectile instanceof AbstractArrow arrow)) return false;

        int extra = clockBonus();
        if (extra <= 0) return false;

        ToolAttackUtil.attackEntitySecondary(attacker.damageSources().arrow(arrow, attacker),
                extra, target, target, true);
        return false;
    }

    /** 真实时间加成：max(小时, 分钟)，近战与远程共用 */
    private static int clockBonus() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return Math.max(now.getHour(), now.getMinute());
    }
}
