package com.galtc2.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 莫尔加那·咒袭（攻击侧）。
 * <p>
 * 只保留攻击钩子：近战命中 / 箭矢命中时给目标挂随机负面效果。
 * 与 {@link MorganaArmorModifier} 成对；{@code curse()} / {@code pool()} / {@code ready()}
 * 等共用逻辑仍留在 {@link MorganaModifier} 里由本类继承。
 */
public class MorganaMeleeModifier extends MorganaModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.PROJECTILE_HIT);
    }
}
