package com.galtc2.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 勺景·后应（防御侧）。
 * <p>
 * 只保留防御钩子：受到伤害时呼唤暗乱反击（暗乱来自 J村，需安装 J村）。
 * 与 {@link ShoujingMeleeModifier} 成对。
 */
public class ShoujingArmorModifier extends ShoujingModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }
}
