package com.galtc2.jvt.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 村民金属·庇护（防御侧）。
 * <p>
 * 只保留防御钩子：附近每只村民 -5% 承伤（封顶 80%，与穿戴件数无关，只结算一次）。
 * 与 {@link VillagerMetalMeleeModifier} 成对。
 */
public class VillagerMetalArmorModifier extends VillagerMetalModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MODIFY_HURT);
    }
}
