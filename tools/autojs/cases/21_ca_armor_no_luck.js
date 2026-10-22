//@steps setup,measure
// CA（护甲侧）反向用例：幸运 0 时不应该白给护甲。
var CASE_DEF = {
    name: '21_ca_armor_no_luck',
    setup: function () {
        GALT.clearArmor();
        GALT.clearLuck();
        GALT.equipChest(GALT.chestplate('galtc2:ca'));
        return GALT.staged(this.name, '幸运 0，已穿上 CA 胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var luck = GALT.player().getAttributeValue(GALT.Attributes.LUCK);
        var bonus = GALT.traitBonus().bonus;
        var base = GALT.armorBase();
        var armor = GALT.armorValue();

        GALT.check(checks, '幸运值', 0, luck);
        GALT.check(checks, '特性护甲', 0, bonus);
        GALT.check(checks, '总护甲(只有胸甲)', base, armor, GALT.near(base, armor, 0.05));
        notes.push('护甲属性修饰器: ' + GALT.traitBonus().modifiers.join(' / '));
        return GALT.report(this.name, checks, notes);
    }
};
