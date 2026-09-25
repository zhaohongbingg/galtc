package com.galtc2.jvt.modifiers;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 明路之心(bright_land_heart)
 * 复刻 J村：佩戴时提升最大生命值(每件 +4)。
 * 无槽位、不互斥。
 */
public class BrightLandHeartModifier extends Modifier implements AttributesModifierHook {

    private static final UUID MAX_HEALTH_UUID = UUID.fromString("5e2f7c1a-3b6d-4a8e-9c1f-2d4b6a8c0e1f");
    private static final double MAX_HEALTH_PER_LEVEL = 4.0;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ATTRIBUTES);
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Attribute, AttributeModifier> consumer) {
        if (slot.isArmor()) {
            consumer.accept(Attributes.MAX_HEALTH, new AttributeModifier(MAX_HEALTH_UUID,
                    "jvt.bright_land_heart", MAX_HEALTH_PER_LEVEL * modifier.getLevel(), AttributeModifier.Operation.ADDITION));
        }
    }
}
