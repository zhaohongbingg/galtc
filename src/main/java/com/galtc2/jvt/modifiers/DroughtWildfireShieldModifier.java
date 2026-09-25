package com.galtc2.jvt.modifiers;

/**
 * 旱火野火盾牌(drought_wildfire_shield)
 * 复刻 J村：长按右键蓄力后松手，施放"蛇龙火舌"法术(jerotesvillage_serpon_flames)。
 * 占用能力槽，且不兼容其他武器能力强化。
 */
public class DroughtWildfireShieldModifier extends ChargedSpellAbilityModifier {

    @Override
    protected String spellId() {
        return "jerotesvillage_serpon_flames";
    }
}
