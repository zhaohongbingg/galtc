package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.util.AttackFind;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

/**
 * 巨怪毛·近战特性(霜噬)
 * 近战命中后，附加与本次伤害等量的冰冻类伤害(使用 J村 冰冻免疫所识别的 jerotes:freeze_melee)。
 * <p>
 * <b>箭矢分支（F-029）</b>：远程走 {@code PROJECTILE_HIT}，附加与<b>该箭 {@code baseDamage} 等量</b>的
 * 冰冻伤害，伤害类型换成 J村 的射击档 {@code jerotes:freeze_shoot}（同样在 {@code minecraft:is_freezing}
 * 标签内，巨怪套/冰系免疫照样认）。追加伤害走 {@link ToolAttackUtil#attackEntitySecondary}，
 * 不在 hook 里调 {@code target.hurt()}，否则会顶满 {@code invulnerableTime} 吃掉箭矢自身伤害。
 * <p>
 * 退化：{@code jerotes:freeze_shoot} 取不到时依次退到 {@code jerotes:freeze_melee}、最后退到匠魂的
 * {@code tconstruct:melee_arrow}（永远不会抛异常、不会刷 ERROR）。
 * 每支箭对同一目标只结算一次（箭矢 {@code persistentData} 标记）。
 */
public class GiantMonsterHairMeleeModifier extends Modifier implements MeleeHitModifierHook, ProjectileHitModifierHook {

    /** J村 冰冻类伤害(近战档，在 minecraft:is_freezing tag 内)，会被巨怪套的减伤识别 */
    private static final ResourceKey<DamageType> FREEZE_MELEE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("jerotes", "freeze_melee"));

    /** J村 冰冻类伤害(射击档，同样在 minecraft:is_freezing tag 内) */
    private static final ResourceKey<DamageType> FREEZE_SHOOT =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("jerotes", "freeze_shoot"));

    /** 箭矢分支的去重标记（写在箭矢的 persistentData 上） */
    private static final ResourceLocation RANGED_DONE =
            ResourceLocation.fromNamespaceAndPath("galtc2", "giant_monster_hair_ranged_done");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (damageDealt <= 0) {
            return;
        }
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        Level level = context.getLevel();
        if (target == null || !target.isAlive() || level.isClientSide) {
            return;
        }
        DamageSource frost = AttackFind.findDamageType(attacker, FREEZE_MELEE, attacker);
        target.hurt(frost, damageDealt);
    }

    /**
     * 箭矢命中：附加与该箭 {@code baseDamage} 等量的冰冻伤害（射击档伤害类型）。
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
        float extra = (float) arrow.getBaseDamage();
        if (extra <= 0 || Float.isNaN(extra) || Float.isInfinite(extra)) {
            return false;
        }
        // 每支箭对同一目标只算一次（一次箭矢已经自带等量本体伤害，不能滚雪球）
        ResourceLocation key = rangedKey(target);
        if (persistentData.getBoolean(key)) {
            return false;
        }
        persistentData.putBoolean(key, true);

        ToolAttackUtil.attackEntitySecondary(frostSource(projectile, attacker), extra, target, target, true);
        return false;
    }

    /**
     * 取冰冻伤害源：优先射击档（{@code jerotes:freeze_shoot}），取不到退近战档，
     * 再取不到退匠魂通用（{@code tconstruct:melee_arrow}）——保证 J村 侧数据缺失时也不抛异常。
     */
    private static DamageSource frostSource(Projectile projectile, LivingEntity attacker) {
        Level level = projectile.level();
        try {
            return AttackFind.findDamageType(projectile, FREEZE_SHOOT, projectile, attacker);
        } catch (RuntimeException ignored) {
            // 落下去尝试下一个
        }
        try {
            return AttackFind.findDamageType(projectile, FREEZE_MELEE, projectile, attacker);
        } catch (RuntimeException ignored) {
            // 落下去用匠魂通用来源
        }
        return TinkerDamageTypes.source(level.registryAccess(), TinkerDamageTypes.MELEE_ARROW, projectile, attacker);
    }

    /** 去重键 = 固定标记 + 命中目标 id（穿射同一支箭打不同目标时各自结算一次） */
    private static ResourceLocation rangedKey(LivingEntity target) {
        return ResourceLocation.fromNamespaceAndPath("galtc2", RANGED_DONE.getPath() + "_" + target.getId());
    }
}
