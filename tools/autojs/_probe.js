// 临时：确认 ARMOR 属性的实际上限（有没有属性突破）
(function () {
    var Class = Java.loadClass('java.lang.Class');
    var AttributeModifier = Java.loadClass('net.minecraft.world.entity.ai.attributes.AttributeModifier');
    var UUID = Java.loadClass('java.util.UUID');
    var p = AutoJS.player();
    var out = {};

    var armor = p.getAttribute(GALT.Attributes.ARMOR);
    var attribute = armor.getAttribute();
    out.attributeClass = String(attribute.getClass().getName());

    try {
        var maxField = Class.forName('net.minecraft.world.entity.ai.attributes.RangedAttribute')
            .getDeclaredField('maxValue');
        maxField.setAccessible(true);
        out.maxValue = maxField.get(attribute);
    } catch (e) {
        out.maxValueError = String(e);
    }
    try {
        var minField = Class.forName('net.minecraft.world.entity.ai.attributes.RangedAttribute')
            .getDeclaredField('minValue');
        minField.setAccessible(true);
        out.minValue = minField.get(attribute);
    } catch (e2) {
        out.minValueError = String(e2);
    }

    var testUuid = UUID.fromString('8f1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d');
    armor.removeModifier(testUuid);
    armor.addTransientModifier(new AttributeModifier(testUuid, 'autojs:probe', 200.0,
        AttributeModifier.Operation.ADDITION));
    out.with200 = GALT.armorValue();
    out.armorInt = p.getArmorValue();
    armor.removeModifier(testUuid);
    out.afterRemove = GALT.armorValue();
    return JSON.stringify(out);
})();
