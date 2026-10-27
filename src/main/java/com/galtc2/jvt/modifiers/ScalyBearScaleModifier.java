package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.util.AttackFind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.common.TinkerDamageTypes;
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

/**
 * 鳞熊鳞片(scaly_bear_scale) 材料特性：<b>对友好生物造成的伤害 +20%（每级）</b>。
 * <p>
 * 「友好」沿用 J村 自己的阵营判定 {@link AttackFind#SameFactionAvoidDamage}——
 * 也就是同阵营、平时会互相避让不打的那些生物。如果以后想换口径
 * （例如改成 {@code AttackFind.FindCanNotAttack} 或某个具体阵营），只改下面这一处判断即可。
 * <p>
 * 数值走 TC 的近战伤害钩子，和 {@link AncientSecondRounderModifier} 同一个套路：
 * 先拿 {@code ToolAttackContext} 里的目标，判定通过后按等级乘算。
 * <p>
 * <b>箭矢分支（F-029）</b>：箭矢走 {@code PROJECTILE_HIT}，追加伤害 = 该箭
 * {@code baseDamage × 0.2 × 等级}（与近战倍率一致），一律交给
 * {@link ToolAttackUtil#attackEntitySecondary} 结算，禁止在本 hook 里 {@code target.hurt()}。
 * 每支箭对同一目标只结算一次（箭矢 {@code persistentData} 标记）。
 */
public class ScalyBearScaleModifier extends Modifier implements MeleeDamageModifierHook, ProjectileHitModifierHook {

    /** 每级对友好生物的额外伤害倍率 */
    private static final float BONUS_PER_LEVEL = 0.2f;

    /** 箭矢分支的去重标记（写在箭矢的 persistentData 上） */
    private static final ResourceLocation RANGED_DONE =
            ResourceLocation.fromNamespaceAndPath("galtc2", "scaly_bear_scale_ranged_done");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target == null || attacker == null || baseDamage <= 0 || damage <= 0) {
            return damage;
        }
        // 只对"友好生物"生效
        if (!AttackFind.SameFactionAvoidDamage(attacker, target, false)) {
            return damage;
        }
        float result = damage * (1.0f + BONUS_PER_LEVEL * modifier.getLevel());
        if (Float.isNaN(result) || Float.isInfinite(result)) {
            return damage;
        }
        return result;
    }

    /**
     * 箭矢命中：对友好生物追加 {@code baseDamage × 0.2 × 等级}（与近战倍率一致）。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         LivingEntity attacker, LivingEntity target) {
        // 服务端权威 + 空值/存活守卫
        if (projectile.level().isClientSide() || target == null || attacker == null || !target.isAlive()) {
            return false;
        }
        if (!(projectile instanceof AbstractArrow arrow)) {
            return false;
        }
        double baseDamage = arrow.getBaseDamage();
        float extra = (float) baseDamage * BONUS_PER_LEVEL * modifier.getLevel();
        if (extra <= 0 || Float.isNaN(extra) || Float.isInfinite(extra)) {
            return false;
        }
        // 只对"友好生物"生效（沿用近战同一套阵营判定）
        if (!AttackFind.SameFactionAvoidDamage(attacker, target, false)) {
            return false;
        }
        // 每支箭对同一目标只算一次
        ResourceLocation key = rangedKey(target);
        if (persistentData.getBoolean(key)) {
            return false;
        }
        persistentData.putBoolean(key, true);

        DamageSource source = TinkerDamageTypes.source(
                projectile.level().registryAccess(), TinkerDamageTypes.MELEE_ARROW, arrow, attacker);
        ToolAttackUtil.attackEntitySecondary(source, extra, target, target, true);
        return false;
    }

    /** 去重键 = 固定标记 + 命中目标 id（穿射同一支箭打不同目标时各自结算一次） */
    private static ResourceLocation rangedKey(LivingEntity target) {
        return ResourceLocation.fromNamespaceAndPath("galtc2", RANGED_DONE.getPath() + "_" + target.getId());
    }
}
