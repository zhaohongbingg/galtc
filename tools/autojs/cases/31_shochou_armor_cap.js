//@steps setup,measure
// 所长（护甲侧）封顶用例：钱币 128 个 → 理论 32 点，实际应被封在 20。
var CASE_DEF = {
    name: '31_shochou_armor_cap',
    setup: function () {
        GALT.clearArmor();
        GALT.fillSlots('minecraft:emerald', 128);
        GALT.equipChest(GALT.chestplate('galtc2:shochou'));
        return GALT.staged(this.name, '塞了 128 个绿宝石，已穿上所长胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var money = GALT.count('minecraft:emerald');
        var bonus = GALT.traitBonus().bonus;
        var base = GALT.armorBase();
        var armor = GALT.armorValue();

        GALT.check(checks, '背包绿宝石数量(含原有)', money, money, true);
        GALT.check(checks, '钱够多，应该吃满封顶 20', 20, bonus, GALT.near(20, bonus, 0.05));
        GALT.check(checks, '总护甲(胸甲+20)', Math.min(30, base + 20), armor,
            GALT.near(Math.min(30, base + 20), armor, 0.05));
        if (money < 80) {
            notes.push('绿宝石只塞进去 ' + money + ' 个，封顶没被测到');
        }
        return GALT.report(this.name, checks, notes);
    }
};
