package com.galtc2.modifiers;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import net.minecraft.world.entity.EquipmentSlot;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.BiConsumer;

public class YidianModifier extends Modifier implements AttributesModifierHook {

    private static final UUID ARMOR_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    public YidianModifier() {}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ATTRIBUTES);
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Attribute, AttributeModifier> consumer) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        
        int larger = Math.max(hour, minute);
        int smaller = Math.min(hour, minute);
        
        consumer.accept(Attributes.ARMOR, new AttributeModifier(ARMOR_UUID, "yidian_armor", larger, AttributeModifier.Operation.ADDITION));
        consumer.accept(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(TOUGHNESS_UUID, "yidian_toughness", smaller, AttributeModifier.Operation.ADDITION));
    }
}