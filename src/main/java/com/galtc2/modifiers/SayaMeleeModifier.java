package com.galtc2.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 沙耶·触袭（攻击侧）。
 * <p>
 * 只保留攻击钩子：近战命中<b>或被箭矢射中</b>的目标附近召唤触手协助攻击（触手来自 J村，需安装 J村）。
 * 与 {@link SayaArmorModifier} 成对。
 * <p>
 * 远程要两个钩子：{@code PROJECTILE_LAUNCH} 把弓的投射物伤害抄到箭矢上，
 * {@code PROJECTILE_HIT} 命中时按它 ×0.5 召唤（实现都在父类 {@link SayaModifier} 里）。
 */
public class SayaMeleeModifier extends SayaModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT,
                ModifierHooks.PROJECTILE_HIT, ModifierHooks.PROJECTILE_LAUNCH);
    }
}
