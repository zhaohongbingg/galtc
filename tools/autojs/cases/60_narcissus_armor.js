//@steps setup,measure
// 水仙（护甲侧 galtc2:narcissus_armor）：免疫「低于自身最大生命一半」的单次伤害。
// 判定发生在护甲减伤之后（NarcissusArmor#onModifyTakeDamage）。
// 玩家最大生命 20、水仙胸甲护甲 5 / 韧性 2 时（面板数值见 stats，运行时按实测算期望）：
//   10 点攻击 → 减伤后 9.6 < 10 → 免疫
//   12 点攻击 → 减伤后 11.5 ≥ 10 → 照吃（扣完还剩 8.5 血，不会被打死）
// 面板上最多能免掉约 10.4 点以内的攻击，比面板一半略宽。
var CASE_DEF = {
    name: '60_narcissus_armor',
    setup: function () {
        GALT.clearArmor();
        GALT.equipChest(GALT.chestplate('galtc2:narcissus'));
        return GALT.staged(this.name, '已穿上水仙胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];
        var p = GALT.player();
        var maxHealth = p.getMaxHealth();
        var threshold = maxHealth * 0.5;

        var stand = GALT.dummy();
        var small = GALT.hurt(threshold * 0.5, stand);
        var half = GALT.hurt(threshold, stand);
        var over = GALT.hurt(threshold + 2.0, stand);
        var expectedOver = GALT.expectedAfterArmor(threshold + 2.0);
        if (stand) {
            stand.discard();
        }

        GALT.check(checks, '最大生命', 20, maxHealth);
        GALT.check(checks, '5 点伤害(减伤后 ' + GALT.expectedAfterArmor(threshold * 0.5).toFixed(2)
            + ' < 阈值 ' + threshold + ')免疫', 0, small, GALT.near(small, 0, 0.001));
        GALT.check(checks, '10 点伤害(减伤后 ' + GALT.expectedAfterArmor(threshold).toFixed(2)
            + ' 仍低于阈值)免疫', 0, half, GALT.near(half, 0, 0.001));
        GALT.check(checks, '12 点伤害(减伤后 ' + expectedOver.toFixed(2) + ' ≥ 阈值)照吃', expectedOver, over,
            GALT.near(over, expectedOver, 0.05));
        // 面板上的免疫上限：最大的 d 使「减伤后仍低于阈值」
        var maxPanel = 0;
        for (var d = 0.1; d < maxHealth; d = Math.round((d + 0.1) * 10) / 10) {
            if (GALT.expectedAfterArmor(d) >= threshold) {
                break;
            }
            maxPanel = d;
        }
        notes.push('阈值判的是护甲减伤后的数值，穿 ' + GALT.armorValue() + ' 点护甲时面板上 ' + maxPanel.toFixed(1)
            + ' 以内的攻击都会被免疫（比面板一半 ' + threshold + ' 略宽）');
        return GALT.report(this.name, checks, notes);
    }
};
