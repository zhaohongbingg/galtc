//@steps measure
// 12 个护甲（防御）特性是否注册、是否被对应材料在 tconstruct:armor 上读到。
// 另外两条原本写在 default 上、后来改成护甲专属的特性也不能漏。
var CASE_DEF = {
    name: '01_armor_traits_registered',
    measure: function () {
        var MaterialRegistry = Java.loadClass('slimeknights.tconstruct.library.materials.MaterialRegistry');
        var MaterialId = Java.loadClass('slimeknights.tconstruct.library.materials.definition.MaterialId');
        var ModifierId = Java.loadClass('slimeknights.tconstruct.library.modifiers.ModifierId');
        var ModifierManager = Java.loadClass('slimeknights.tconstruct.library.modifiers.ModifierManager');

        var expected = {
            sprb: 'sprb_armor',
            zeroone: 'zeroone_armor',
            narcissus: 'narcissus_armor',
            ca: 'ca_armor',
            azarashi: 'azarashi_armor',
            nighttimesheep: 'nighttimesheep_armor',
            kisaragikurenai: 'kisaragikurenai_armor',
            blacklily: 'blacklily_armor',
            shochou: 'shochou_armor',
            ciallo: 'ciallo_armor',
            ancient_second_rounder: 'ancient_second_rounder_armor',
            scaly_bear_scale: 'scaly_bear_scale_armor'
        };

        var registry = MaterialRegistry.getInstance();
        var manager = ModifierManager.INSTANCE;
        var armorStat = MaterialRegistry.ARMOR;
        var checks = [];
        var notes = [];

        function traitIds(list) {
            var ids = [];
            for (var i = 0; i < list.size(); i++) {
                ids.push(String(list.get(i).getId()));
            }
            return ids;
        }

        var passed = 0;
        var total = 0;
        for (var material in expected) {
            total++;
            var trait = 'galtc2:' + expected[material];
            var registered = manager.contains(new ModifierId(trait));
            var armorTraits = traitIds(registry.getTraits(new MaterialId('galtc2:' + material), armorStat));
            var present = armorTraits.indexOf(trait) >= 0;
            if (registered && present) {
                passed++;
            } else {
                notes.push(material + ': 注册=' + registered + ' 护甲特性=' + present
                    + '（实际：' + armorTraits.join(',') + '）');
            }
        }
        GALT.check(checks, '护甲特性注册 + 材料读取', total, passed, passed === total);

        var moved = [['meror', 'galtc2:meror_resistance'], ['refine_meror', 'galtc2:meror_standard']];
        var movedOk = 0;
        for (var j = 0; j < moved.length; j++) {
            var materialId = new MaterialId('galtc2:' + moved[j][0]);
            var defaults = traitIds(registry.getDefaultTraits(materialId));
            var armor = traitIds(registry.getTraits(materialId, armorStat));
            var ok = defaults.indexOf(moved[j][1]) < 0 && armor.indexOf(moved[j][1]) >= 0;
            if (ok) {
                movedOk++;
            } else {
                notes.push(moved[j][0] + ': ' + moved[j][1] + ' 位置不对（default=' + defaults.join(',')
                    + ' armor=' + armor.join(',') + '）');
            }
        }
        GALT.check(checks, '改到护甲专属的两条特性', moved.length, movedOk, movedOk === moved.length);
        return GALT.report(this.name, checks, notes);
    }
};
