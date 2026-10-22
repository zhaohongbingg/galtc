//@steps measure
// 对照：不穿护甲、一次 10 点生物攻击伤害应该按原版护甲公式结算。
// 这条同时验证脚手架本身（生存模式、无敌帧清零、攻击通道正确）。
var CASE_DEF = {
    name: '00_base_control',
    measure: function () {
        var checks = [];
        var notes = [];
        var damage = 10.0;

        GALT.clearArmor();
        var stand = GALT.dummy();
        var taken = GALT.hurt(damage, stand);
        var expected = GALT.expectedAfterArmor(damage);
        if (stand) {
            stand.discard();
        }

        GALT.check(checks, '裸装护甲值', 0, GALT.armorValue());
        GALT.check(checks, '裸装受伤(原版公式)', expected, taken, GALT.near(taken, expected, 0.05));
        if (!GALT.near(GALT.armorValue(), 0, 0.001)) {
            notes.push('裸装护甲不是 0：玩家身上还有常驻护甲来源，后续用例的「基础护甲」以实测为准');
        }
        return GALT.report(this.name, checks, notes);
    }
};
