package com.galtc2.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 沙耶·触袭（攻击侧）。
 * <p>
 * 只保留攻击钩子：近战命中后在目标附近召唤触手协助攻击（触手来自 J村，需安装 J村）。
 * 与 {@link SayaArmorModifier} 成对。
 */
public class SayaMeleeModifier extends SayaModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }
}
