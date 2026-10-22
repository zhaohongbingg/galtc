//@steps setup,measure
// 古二轮金属（护甲侧 galtc2:ancient_second_rounder_armor）：
// 来自 jerotes「复魂尸 / 沉降类」生物的伤害 -25%，其它来源不减。
// 判定用 OtherEntityFactionFind.isResurrecter(attacker.getType())，作用在护甲减免之前。
var CASE_DEF = {
    name: '86_ancient_second_rounder_armor',
    setup: function () {
        GALT.clearArmor();
        GALT.equipChest(GALT.chestplate('galtc2:ancient_second_rounder'));
        return GALT.staged(this.name, '已穿上古二轮胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var base = GALT.expectedAfterArmor(10.0);

        var zombie = GALT.summon('minecraft:zombie', 3, 2);
        if (!zombie) {
            notes.push('僵尸没召唤出来，对照分支没测');
        } else {
            var fromZombie = GALT.hurt(10.0, zombie);
            GALT.check(checks, '普通敌对生物受伤(不减伤)', base, fromZombie, GALT.near(fromZombie, base, 0.05));
            zombie.discard();
        }

        var resurrecter = GALT.summon('jerotesvillage:resurrecter', 3, -2);
        if (!resurrecter) {
            notes.push('jerotesvillage:resurrecter 没召唤出来，减伤分支没测');
        } else {
            var fromResurrecter = GALT.hurt(10.0, resurrecter);
            GALT.check(checks, '复魂尸受伤(×(1-25%)，护甲前结算)',
                GALT.expectedPreReduction(10.0, 0.25), fromResurrecter,
                GALT.near(fromResurrecter, GALT.expectedPreReduction(10.0, 0.25), 0.05));
            resurrecter.discard();
        }
        notes.push('armor=' + GALT.armorValue() + ' toughness=' + GALT.toughness()
            + ' base(10)=' + base);
        return GALT.report(this.name, checks, notes);
    }
};
