//@steps setup,measure
// 月影魅像（护甲侧 galtc2:kisaragikurenai_armor）：只对蜘蛛 / 洞穴蜘蛛的伤害减 50%。
var CASE_DEF = {
    name: '70_kisaragikurenai_armor',
    setup: function () {
        GALT.clearArmor();
        GALT.equipChest(GALT.chestplate('galtc2:kisaragikurenai'));
        return GALT.staged(this.name, '已穿上月影魅像胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var base = GALT.expectedAfterArmor(10.0);

        var stand = GALT.dummy();
        var fromStand = GALT.hurt(10.0, stand);
        if (stand) {
            stand.discard();
        }
        GALT.check(checks, '非蜘蛛伤害(不减伤)', base, fromStand, GALT.near(fromStand, base, 0.05));

        var spider = GALT.summon('minecraft:spider', 3, 0);
        if (!spider) {
            notes.push('蜘蛛没召唤出来，减伤部分没测');
        } else {
            var fromSpider = GALT.hurt(10.0, spider);
            GALT.check(checks, '蜘蛛伤害(×0.5)', base * 0.5, fromSpider, GALT.near(fromSpider, base * 0.5, 0.05));
            spider.discard();
        }

        var caveSpider = GALT.summon('minecraft:cave_spider', 3, 2);
        if (caveSpider) {
            var fromCave = GALT.hurt(10.0, caveSpider);
            GALT.check(checks, '洞穴蜘蛛伤害(×0.5)', base * 0.5, fromCave, GALT.near(fromCave, base * 0.5, 0.05));
            caveSpider.discard();
        }
        return GALT.report(this.name, checks, notes);
    }
};
