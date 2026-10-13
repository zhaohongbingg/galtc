package com.galtc2.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 沙耶·触盾（防御侧）。
 * <p>
 * 只保留防御钩子：受到伤害后召唤触手反击（触手来自 J村，需安装 J村）。
 * 与 {@link SayaMeleeModifier} 成对。
 */
public class SayaArmorModifier extends SayaModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }
}
