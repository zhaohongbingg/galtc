package com.galtc2.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 卡缪斯·咒御（防御侧）。
 * <p>
 * 只保留防御钩子：天气良好时，受到伤害给攻击者下"禁疗"诅咒。
 * 与 {@link CamusMeleeModifier} 成对。
 */
public class CamusArmorModifier extends CamusModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }
}
