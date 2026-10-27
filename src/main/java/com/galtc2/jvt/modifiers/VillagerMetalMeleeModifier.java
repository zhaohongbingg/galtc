package com.galtc2.jvt.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 村民金属·乡勇（攻击侧）。
 * <p>
 * 只保留攻击钩子：附近每只村民 +5% 近战伤害。
 * 与 {@link VillagerMetalArmorModifier} 成对，由材料以 {@code perStat} 分别挂到
 * 武器件（{@code tconstruct:melee_harvest}）与护甲件（{@code tconstruct:armor}）。
 * <p>
 * <b>箭矢分支（F-029 新增 / F-031 改口径）</b>：额外注册 {@code PROJECTILE_HIT}，实现写在父类
 * {@link VillagerMetalModifier#onProjectileHitEntity}（那里能直接用 {@code countVillagers}）。
 * 远程额外伤害 = 周围村民数量（16 格内、上限 16）× 等级 的<b>品脱伤害</b>，
 * 与近战 {@code +5%/村民} 的百分比口径不同（用户口径：「这个材料和村民有关系，所以是周围村民的数量」）。
 */
public class VillagerMetalMeleeModifier extends VillagerMetalModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.PROJECTILE_HIT);
    }
}
