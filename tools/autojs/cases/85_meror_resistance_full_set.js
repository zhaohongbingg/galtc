//@steps setup,measure
// 莫厄减伤（护甲侧）：四件 1 级叠加成 4 级 → 减伤 48%。
// 同时确认「只在第一件上结算」没有把四件各算一遍（那样会变成 0.88^4≈0.60 的连乘）。
var CASE_DEF = {
    name: '85_meror_resistance_full_set',
    setup: function () {
        GALT.clearArmor();
        GALT.equipFullSet('galtc2:meror');
        return GALT.staged(this.name, '已穿满莫厄四件套');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var stand = GALT.dummy();
        var taken = GALT.hurt(10.0, stand);
        if (stand) {
            stand.discard();
        }

        GALT.check(checks, '四件套总护甲(2+7+5+2)', 16, GALT.armorValue());
        GALT.check(checks, '四件套受伤(×(1-48%)，护甲前结算)', GALT.expectedPreReduction(10.0, 0.48), taken,
            GALT.near(taken, GALT.expectedPreReduction(10.0, 0.48), 0.05));
        notes.push('armor=' + GALT.armorValue() + ' toughness=' + GALT.toughness()
            + ' 无减伤=' + GALT.expectedAfterArmor(10.0) + ' 实测=' + taken);
        notes.push('若四件各算一次 12% 连乘会得到 ' + (GALT.expectedAfterArmor(10.0) * 0.88 * 0.88 * 0.88 * 0.88)
            + ' 左右，实测能区分这两种实现');
        return GALT.report(this.name, checks, notes);
    }
};
