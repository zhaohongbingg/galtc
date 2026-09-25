package com.galtc2.jvt.modifiers;

/**
 * 骇浪三叉戟(seawave_trident)
 * 复刻 J村：长按右键蓄力后松手，施放"宝石海浪"法术(jerotesvillage_gemstone_waves)。
 * 占用能力槽，且不兼容其他武器能力强化。
 */
public class SeawaveTridentModifier extends ChargedSpellAbilityModifier {

    @Override
    protected String spellId() {
        return "jerotesvillage_gemstone_waves";
    }
}
