package com.galtc2.modifiers;

import slimeknights.tconstruct.library.modifiers.Modifier;

/**
 * 天津罪·其二 (tianjin_zui_no_spawn) —— 材料特性。
 * <p>
 * 效果是「领域内生物无法生成」。它和 {@link TianjinZuiModifier} 的禁飞共用同一片领域，
 * 但拦的是<b>刷怪</b>，属于全局事件，所以逻辑不在这里 —— 见
 * {@code com.galtc2.event.TianjinZuiSpawnBanEvents}。
 * <p>
 * 这个类的作用就是个「挂载点」：特性必须真的挂在工具/护甲上，事件那边才能用
 * {@code ModifierUtil.getModifierLevel} 查出谁正开着领域。所以它不需要注册任何 hook。
 * <p>
 * 拆成独立特性（而不是塞进 {@link TianjinZuiModifier}）是为了在匠魂书里显示成两条，
 * 也方便以后单独调其中一条。
 */
public class TianjinZuiSpawnBanModifier extends Modifier {

    public TianjinZuiSpawnBanModifier() {
    }
}
