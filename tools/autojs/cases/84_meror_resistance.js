//@steps setup,measure
// 莫厄减伤（护甲侧 galtc2:meror_resistance）：非莫厄来源每级 -12%。
// 单件（1 级）→ ×0.88，作用在护甲减免之前；四件套叠加到 4 级见 85 用例。
var CASE_DEF = {
    name: '84_meror_resistance',
    setup: function () {
        GALT.clearArmor();
        GALT.equipChest(GALT.chestplate('galtc2:meror'));
        return GALT.staged(this.name, '已穿上莫厄胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var stand = GALT.dummy();
        var taken = GALT.hurt(10.0, stand);
        if (stand) {
            stand.discard();
        }

        GALT.check(checks, '总护甲(只有胸甲)', 7, GALT.armorValue());
        GALT.check(checks, '单件受伤(×(1-12%)，护甲前结算)', GALT.expectedPreReduction(10.0, 0.12), taken,
            GALT.near(taken, GALT.expectedPreReduction(10.0, 0.12), 0.05));
        notes.push('判定用 EntityAndItemFind.isMerorAttack(source)，普通生物攻击不算莫厄来源；'
            + 'armor=' + GALT.armorValue() + ' toughness=' + GALT.toughness());
        return GALT.report(this.name, checks, notes);
    }
};
