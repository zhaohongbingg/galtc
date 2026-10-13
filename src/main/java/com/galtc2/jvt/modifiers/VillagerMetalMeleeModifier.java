package com.galtc2.jvt.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 村民金属·乡勇（攻击侧）。
 * <p>
 * 只保留攻击钩子：附近每只村民 +5% 近战伤害。
 * 与 {@link VillagerMetalArmorModifier} 成对，由材料以 {@code perStat} 分别挂到
 * 武器件（{@code tconstruct:melee_harvest}）与护甲件（{@code tconstruct:armor}）。
 */
public class VillagerMetalMeleeModifier extends VillagerMetalModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }
}
