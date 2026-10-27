package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

public class VtbModifier extends Modifier implements MeleeDamageModifierHook, ProjectileHitModifierHook {

    /** 近战：攻击者最大生命的 50% */
    private static final float MELEE_MAX_HEALTH_RATIO = 0.5F;
    /**
     * 远程：攻击者最大生命的 25%。
     * 箭矢本身的基数小（普通弓 ~6 点），照搬 50% 会让一条特性比箭伤还猛，所以远程单独减半。
     * 近战数值不动。
     */
    private static final float RANGED_MAX_HEALTH_RATIO = 0.25F;

    public VtbModifier() {}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if (context.getAttacker() == null || context.getAttacker().level().isClientSide()) {
            return damage;
        }

        LivingEntity attacker = context.getAttacker();

        // 获取玩家的最大生命值
        float maxHealth = attacker.getMaxHealth();

        // 附加额外伤害（最大生命值的50%）
        float extraDamage = maxHealth * MELEE_MAX_HEALTH_RATIO;

        return damage + extraDamage;
    }

    /**
     * 箭矢命中：追加「攻击者最大生命 × {@value #RANGED_MAX_HEALTH_RATIO}」的伤害。
     * <p>
     * 走 {@code attackEntitySecondary}：不写 {@code target.hurt()}，箭矢自身伤害照常结算；
     * 追加伤害挂在箭矢来源上（{@code arrow(arrow, attacker)}），击杀统计/仇恨仍归射手。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null) return false;
        if (attacker.level().isClientSide() || target.isDeadOrDying()) return false;
        if (!(projectile instanceof AbstractArrow arrow)) return false;

        float extra = attacker.getMaxHealth() * RANGED_MAX_HEALTH_RATIO;
        if (extra <= 0.0F) return false;

        ToolAttackUtil.attackEntitySecondary(attacker.damageSources().arrow(arrow, attacker),
                extra, target, target, true);
        return false;
    }
}
