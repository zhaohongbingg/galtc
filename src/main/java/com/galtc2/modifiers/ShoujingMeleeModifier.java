package com.galtc2.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 勺景·前呼（攻击侧）。
 * <p>
 * 只保留攻击钩子：近战命中时呼唤暗乱助战（暗乱来自 J村，需安装 J村）。
 * 与 {@link ShoujingArmorModifier} 成对。
 */
public class ShoujingMeleeModifier extends ShoujingModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }
}
