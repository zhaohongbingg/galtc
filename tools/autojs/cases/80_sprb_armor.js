//@steps measure
// SPRB（护甲侧 galtc2:sprb_armor）：每级 +2 防御槽位。
// 直接读工具栈 NBT 里的 tic_volatile_data.defense，和同部位、无该特性的胸甲做对比。
var CASE_DEF = {
    name: '80_sprb_armor',
    measure: function () {
        var checks = [];
        var notes = [];

        function defenseOf(stack) {
            var found = /defense:(\d+)/.exec(GALT.stackTag(stack));
            return found ? Number(found[1]) : -1;
        }

        var sprb = defenseOf(GALT.chestplate('galtc2:sprb'));
        var control = defenseOf(GALT.chestplate('galtc2:meror'));

        GALT.check(checks, '对照胸甲防御槽', 3, control);
        GALT.check(checks, 'SPRB 胸甲防御槽(对照+2)', control + 2, sprb);

        var plain = /upgrades:(\d+)/.exec(GALT.stackTag(GALT.chestplate('galtc2:sprb')));
        notes.push('SPRB 只给护甲加防御槽，升级/能力槽不变（upgrades=' + (plain ? plain[1] : '?') + '）');
        return GALT.report(this.name, checks, notes);
    }
};
