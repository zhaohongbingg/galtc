//@steps setup,measure
// 鳞熊鳞片（护甲侧 galtc2:scaly_bear_scale_armor）：来自「非敌对」生物的伤害每级 -20%。
// 判定是 `!(attacker instanceof Enemy)`，所以中立 / 友好生物都算，僵尸类（Enemy）不减伤。
// 这条走 ModifyDamageModifierHook，作用在护甲减免之前（先砍数值再算护甲）。
var CASE_DEF = {
    name: '83_scaly_bear_scale_armor',
    setup: function () {
        GALT.clearArmor();
        GALT.equipChest(GALT.chestplate('galtc2:scaly_bear_scale'));
        return GALT.staged(this.name, '已穿上鳞熊鳞片胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var base = GALT.expectedAfterArmor(10.0);

        // 中立攻击者（盔甲架不属于 Enemy）
        var stand = GALT.dummy();
        var fromNeutral = GALT.hurt(10.0, stand);
        if (stand) {
            stand.discard();
        }
        GALT.check(checks, '中立攻击者受伤(×(1-20%)，护甲前结算)',
            GALT.expectedPreReduction(10.0, 0.2), fromNeutral,
            GALT.near(fromNeutral, GALT.expectedPreReduction(10.0, 0.2), 0.05));

        // 敌对攻击者（僵尸属于 Enemy）
        var zombie = GALT.summon('minecraft:zombie', 3, 2);
        if (!zombie) {
            notes.push('僵尸没召唤出来，敌对分支没测');
        } else {
            var fromEnemy = GALT.hurt(10.0, zombie);
            GALT.check(checks, '敌对攻击者受伤(不减伤)', base, fromEnemy, GALT.near(fromEnemy, base, 0.05));
            zombie.discard();
        }
        notes.push('armor=' + GALT.armorValue() + ' toughness=' + GALT.toughness()
            + ' base(10)=' + base);
        return GALT.report(this.name, checks, notes);
    }
};
