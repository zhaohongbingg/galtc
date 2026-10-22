//@steps setup,measure
// 海豹（护甲侧 galtc2:azarashi_armor）：读到的是四件护甲合起来的面板总和，
// 面板 > 50 时额外补「面板 × 2」，等效总护甲 ×3。
// 判定用的是 ArmorAttributeHelper.baseArmor()：按 ARMOR 修饰器逐项累加、跳过自己那一份，
// 拿到的是没有被属性上限截断的基础值，所以这里必须用 GALT.baseArmor() 而不是 getArmorValue()。
// 原版 Attributes.ARMOR 上限 30，只有装了属性突破把上限抬高之后才可能触及 50：
//   面板能超过 50 → 应该 ×3；面板够不到 50 → 特性必须保持 0 加成（不能误触发）。
var CASE_DEF = {
    name: '50_azarashi_armor',
    setup: function () {
        GALT.clearArmor();
        GALT.equipChest(GALT.chestplate('galtc2:azarashi'));
        // 只有上限够高（≥ 170）才把面板顶到刚好 55：
        // 顶太高的话「基础 ×3」会撞上属性上限，可见护甲就不会是 ×3 了。
        var cap = GALT.armorCap();
        var chest = GALT.baseArmor();
        GALT.addArmorBoost(cap >= 170 ? 55 - chest : 0);
        return GALT.staged(this.name, 'ARMOR 上限 ' + (isFinite(cap) ? cap : '未知')
            + '，已穿上海豹胸甲' + (cap >= 170 ? '，并把面板顶到 55' : '（上限不足，只验证不误触发）'));
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var bonus = GALT.traitBonus().bonus;
        var cap = GALT.armorCap();
        var known = cap > 0 && isFinite(cap);   // 读不到上限时不做截断推断
        var raw = GALT.armorBase();             // 修饰器原始总和（未截断）
        var base = GALT.baseArmor();            // 特性判定看到的基础护甲
        var armor = GALT.armorValue();
        var stand = GALT.dummy();
        var taken = GALT.hurt(10.0, stand);
        var expected = GALT.expectedAfterArmor(10.0);
        if (stand) {
            stand.discard();
        }

        GALT.check(checks, '基础护甲(全部护甲合计 ' + raw + '，上限 ' + (known ? cap : '未知') + ')', raw, base,
            GALT.near(raw, base, 0.5));

        var expectedBonus = base > 50 ? base * 2 : 0;
        GALT.check(checks, '特性护甲(基础 ' + base + '，' + (base > 50 ? '×2' : '未过阈值 → 0') + ')',
            expectedBonus, bonus, GALT.near(expectedBonus, bonus, 0.05));
        var expectedTotal = known ? Math.min(base + expectedBonus, cap) : base + expectedBonus;
        GALT.check(checks, '总护甲(基础+特性，受上限截断)', expectedTotal, armor,
            GALT.near(expectedTotal, armor, 0.05));
        GALT.check(checks, '受伤(原版护甲公式)', expected, taken, GALT.near(taken, expected, 0.05));

        if (base > 50) {
            notes.push('属性突破生效了：面板 ' + base + ' 超过 50，这条特性可以触发');
        } else {
            notes.push('当前环境 ARMOR 上限 ' + (known ? cap : '未知') + '，面板只到 ' + base
                + '；要触发特性得把上限抬到 50 以上，而「基础 ×3」不被截断需要上限 ≥ 170'
                + '（判定读的就是四件护甲合起来的面板总和）');
        }
        return GALT.report(this.name, checks, notes);
    }
};
