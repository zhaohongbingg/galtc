//@steps setup,measure
// 夜羊（护甲侧 galtc2:nighttimesheep_armor）：防御不足 18 时补足差额。
// 设计：护甲 7 的胸甲 → 补 11 → 总护甲 18；受伤 = 原版护甲公式(10, 18, 韧性)。
var CASE_DEF = {
    name: '10_nighttimesheep_armor',
    setup: function () {
        GALT.clearArmor();
        GALT.equipChest(GALT.chestplate('galtc2:nighttimesheep'));
        return GALT.staged(this.name, '已穿上夜羊胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var bonus = GALT.traitBonus().bonus;
        var armor = GALT.armorValue();
        var base = armor - bonus;
        var stand = GALT.dummy();
        var taken = GALT.hurt(10.0, stand);
        var expected = GALT.expectedAfterArmor(10.0);
        if (stand) {
            stand.discard();
        }

        GALT.check(checks, '胸甲基础护甲', 7, base);
        GALT.check(checks, '特性补的护甲(18-基础)', 18 - base, bonus);
        GALT.check(checks, '总护甲', 18, armor);
        GALT.check(checks, '受伤(原版护甲公式)', expected, taken, GALT.near(taken, expected, 0.05));
        notes.push('护甲属性修饰器: ' + GALT.traitBonus().modifiers.join(' / '));
        return GALT.report(this.name, checks, notes);
    }
};
