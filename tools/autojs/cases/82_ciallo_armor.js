//@steps setup,measure
// 丛雨丸（护甲侧 galtc2:ciallo_armor）：被打时播放 Ciallo 音效。
// 音效本身没法无头断言，这里只保证：受击时钩子能跑通（抛异常会让整个用例报错）、
// 且不会顺带改动伤害结算。
var CASE_DEF = {
    name: '82_ciallo_armor',
    setup: function () {
        GALT.clearArmor();
        GALT.equipChest(GALT.chestplate('galtc2:ciallo'));
        return GALT.staged(this.name, '已穿上丛雨丸胸甲');
    },
    measure: function () {
        var checks = [];
        var notes = [];

        var expected = GALT.expectedAfterArmor(10.0);
        var stand = GALT.dummy();
        var taken = GALT.hurt(10.0, stand);
        var invulnerable = GALT.player().invulnerableTime;
        if (stand) {
            stand.discard();
        }

        GALT.check(checks, '受击后受伤(原版公式，音效不影响数值)', expected, taken, GALT.near(taken, expected, 0.05));
        GALT.check(checks, '受击后无敌帧(原版 0，因为没有走 hurt())', 0, invulnerable);
        notes.push('只验证了「受击时钩子不炸 + 不篡改伤害」；音效需要人在客户端听，'
            + '或者监听 ClientboundSoundPacket 才能自动断言');
        return GALT.report(this.name, checks, notes);
    }
};
