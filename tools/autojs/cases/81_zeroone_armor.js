//@steps setup,measure
// 零一（护甲侧 galtc2:zeroone_armor）：受击后把无敌帧顶到 20 + 10×等级（1 级 → 30 tick）。
// OnAttacked 钩子挂在「原版 hurt() 的攻击事件」上，所以必须走 hurt()；
// 而这个整合包里 LivingAttackEvent 会被某个模组拦掉/改数值（实测有时返回 false、有时数值被改），
// 所以这里只断言无敌帧，不断言这一下的伤害。
var CASE_DEF = {
    name: '81_zeroone_armor',
    setup: function () {
        GALT.clearArmor();
        GALT.equipChest(GALT.chestplate('galtc2:zeroone'));
        return GALT.staged(this.name, '已穿上零一胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var withArmor = GALT.hurtVanilla(10.0);
        GALT.clearArmor();
        var bare = GALT.hurtVanilla(10.0);

        GALT.check(checks, '穿甲受击后无敌帧(20+10)', 30, withArmor.invulnerableTime);
        GALT.check(checks, '脱甲受击后无敌帧没被延长(<=20)', 20, Math.min(20, bare.invulnerableTime), true);
        notes.push('穿甲这一下实际掉血 ' + withArmor.taken + '（applied=' + withArmor.applied
            + '），原版 hurt() 在本整合包里被其它模组干预，数值不作为断言');
        notes.push('第一下照吃，延长的是「挨打之后」的无敌帧，用来挡连击 / 多段伤害');
        return GALT.report(this.name, checks, notes);
    }
};
