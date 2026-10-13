package com.galtc2.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 莫尔加那·咒返（防御侧）。
 * <p>
 * 只保留防御钩子：受到伤害时给攻击者挂随机负面效果。
 * 与 {@link MorganaMeleeModifier} 成对。
 */
public class MorganaArmorModifier extends MorganaModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }
}
