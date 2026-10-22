//@steps setup,measure
// 所长（护甲侧 galtc2:shochou_armor）：背包里每个钱币 +0.25 护甲，封顶 +20。
// 钱币物品默认 minecraft:emerald（GaltcConfig.shochouMoneyItem）。
var CASE_DEF = {
    name: '30_shochou_armor',
    setup: function () {
        GALT.clearArmor();
        GALT.fillSlots('minecraft:emerald', 40);
        GALT.equipChest(GALT.chestplate('galtc2:shochou'));
        return GALT.staged(this.name, '塞了 40 个绿宝石，已穿上所长胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var money = GALT.count('minecraft:emerald');
        var expectedBonus = Math.min(20, money * 0.25);
        var bonus = GALT.traitBonus().bonus;
        var base = GALT.armorBase();
        var armor = GALT.armorValue();

        GALT.check(checks, '背包绿宝石数量(含原有)', money, money, true);
        GALT.check(checks, '特性护甲(min(20, 数量×0.25))', expectedBonus, bonus, GALT.near(expectedBonus, bonus, 0.05));
        GALT.check(checks, '总护甲(胸甲+特性)', Math.min(30, base + expectedBonus), armor,
            GALT.near(Math.min(30, base + expectedBonus), armor, 0.05));
        notes.push('胸甲基础护甲 ' + base + '，护甲属性修饰器: ' + GALT.traitBonus().modifiers.join(' / '));
        return GALT.report(this.name, checks, notes);
    }
};
