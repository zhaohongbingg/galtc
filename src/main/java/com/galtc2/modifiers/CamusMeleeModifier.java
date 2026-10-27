package com.galtc2.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHookMap;

/**
 * 卡缪斯·晴咒（攻击侧）。
 * <p>
 * 只保留攻击钩子：天气良好时，近战命中<b>或被箭矢射中</b>的目标获得"禁疗"诅咒。
 * 与 {@link CamusArmorModifier} 成对；诅咒的实际效果（{@code onLivingHeal} 取消治疗）
 * 是全局事件监听，与哪一侧无关，仍在 {@link CamusModifier} 里只挂一次。
 * <p>
 * 箭矢分支的实现放在父类 {@link CamusModifier#onProjectileHitEntity}，
 * 这里只多注册一个 {@code PROJECTILE_HIT} 钩子（护甲侧不注册，所以不会误触发）。
 */
public class CamusMeleeModifier extends CamusModifier {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.PROJECTILE_HIT);
    }
}
