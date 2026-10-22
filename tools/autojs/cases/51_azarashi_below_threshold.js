//@steps setup,measure
// 海豹（护甲侧）反向用例：面板护甲（全部护甲合计）没超过 50 时不应该加成。
var CASE_DEF = {
    name: '51_azarashi_below_threshold',
    setup: function () {
        GALT.clearArmor();
        GALT.clearBoost();
        GALT.equipChest(GALT.chestplate('galtc2:azarashi'));
        return GALT.staged(this.name, '只有胸甲，已穿上海豹胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var bonus = GALT.traitBonus().bonus;
        var base = GALT.baseArmor();
        var armor = GALT.armorValue();

        GALT.check(checks, '基础护甲(只有胸甲)', base, armor, GALT.near(base, armor, 0.05));
        GALT.check(checks, '特性护甲(未过阈值应为 0)', 0, bonus);
        notes.push('胸甲基础护甲 ' + base + '，护甲属性修饰器: ' + GALT.traitBonus().modifiers.join(' / '));
        return GALT.report(this.name, checks, notes);
    }
};
