//@steps setup,measure
// CA（护甲侧 galtc2:ca_armor）：护甲 = 幸运²（幸运夹在 ±10）。
// 幸运 +3 → 9 点额外护甲。注意 Attributes.ARMOR 取值上限是 30，超出的部分只是数字。
var CASE_DEF = {
    name: '20_ca_armor',
    setup: function () {
        GALT.clearArmor();
        GALT.clearLuck();
        GALT.addLuck(3);
        GALT.equipChest(GALT.chestplate('galtc2:ca'));
        return GALT.staged(this.name, '幸运 +3，已穿上 CA 胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var luck = GALT.player().getAttributeValue(GALT.Attributes.LUCK);
        var bonus = GALT.traitBonus().bonus;
        var base = GALT.armorBase();
        var armor = GALT.armorValue();
        var stand = GALT.dummy();
        var taken = GALT.hurt(10.0, stand);
        var expected = GALT.expectedAfterArmor(10.0);
        if (stand) {
            stand.discard();
        }

        GALT.check(checks, '幸运值', 3, luck);
        GALT.check(checks, '特性护甲(幸运²)', luck * luck, bonus, GALT.near(luck * luck, bonus, 0.05));
        GALT.check(checks, '实际生效护甲(封顶 30)', Math.min(30, base + luck * luck), armor,
            GALT.near(Math.min(30, base + luck * luck), armor, 0.05));
        GALT.check(checks, '受伤(原版护甲公式)', expected, taken, GALT.near(taken, expected, 0.05));
        notes.push('面板上限 30 点：幸运 ≥ 5 时 25 + 胸甲 7 就已经顶满，'
            + '幸运 ±10 的 100 点护甲只在水晶/属性突破这类抬高上限的环境里才有意义');
        return GALT.report(this.name, checks, notes);
    }
};
