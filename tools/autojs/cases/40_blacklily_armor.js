//@steps setup,warmup,measure
// 黑莉莉（护甲侧 galtc2:blacklily_armor）两段设计：
//   1) 护甲 = 已损失生命比例 × 15；
//   2) 受击时按攻击者条件减伤：攻击者最大生命更高 +15%、攻击者护甲更高 +15%，上限 30%。
var CASE_DEF = {
    name: '40_blacklily_armor',
    setup: function () {
        GALT.clearArmor();
        GALT.equipChest(GALT.chestplate('galtc2:blacklily'));
        return GALT.staged(this.name, '已穿上黑莉莉胸甲');
    },
    warmup: function () {
        GALT.setHealthRatio(0.5);
        return GALT.staged(this.name, '血量压到 50%');
    },
    measure: function () {
        var checks = [];
        var notes = [];
        var p = GALT.player();

        // ---- 护甲部分：按当前血量算期望，避免自然回血带来的漂移 ----
        var health = p.getHealth();
        var maxHealth = p.getMaxHealth();
        var lostRatio = 1.0 - health / maxHealth;
        var expectedBonus = lostRatio * 15;
        var bonus = GALT.traitBonus().bonus;
        var base = GALT.armorBase();
        var armor = GALT.armorValue();
        GALT.check(checks, '已损失生命比例', lostRatio, lostRatio, true);
        GALT.check(checks, '特性护甲(损失比例×15)', expectedBonus, bonus, GALT.near(expectedBonus, bonus, 1.0));
        GALT.check(checks, '总护甲(胸甲+特性)', Math.min(30, base + expectedBonus), armor,
            GALT.near(Math.min(30, base + expectedBonus), armor, 1.0));

        var base0 = GALT.expectedAfterArmor(10.0);

        // ---- 减伤部分 A：中立攻击者（血量/护甲都不高于玩家）→ 不减伤 ----
        var stand = GALT.dummy();
        var takenA = GALT.hurt(10.0, stand);
        if (stand) {
            stand.discard();
        }
        GALT.check(checks, '中立攻击者受伤(=护甲公式)', base0, takenA, GALT.near(takenA, base0, 0.05));

        // ---- 减伤部分 B：高生命攻击者（铁傀儡 100 血）→ 15% ----
        var golem = GALT.summon('minecraft:iron_golem', 3, 2);
        if (!golem) {
            notes.push('铁傀儡没召唤出来，减伤部分没测');
        } else {
            var takenB = GALT.hurt(10.0, golem);
            GALT.check(checks, '高生命攻击者受伤(×(1-15%))', base0 * 0.85, takenB,
                GALT.near(takenB, base0 * 0.85, 0.05));

            // ---- 减伤部分 C：高生命 + 高护甲 → 15%+15% = 30% ----
            GALT.boostEntityArmor(golem, 50);
            var takenC = GALT.hurt(10.0, golem);
            GALT.check(checks, '高生命+高护甲攻击者受伤(×(1-30%))', base0 * 0.70, takenC,
                GALT.near(takenC, base0 * 0.70, 0.05));
            golem.discard();
        }
        notes.push('减伤是乘在「护甲减伤之后」的数值上，所以和护甲是叠乘关系');
        return GALT.report(this.name, checks, notes);
    }
};
