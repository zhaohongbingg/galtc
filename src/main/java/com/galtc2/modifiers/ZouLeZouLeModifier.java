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
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;
import java.util.function.BiConsumer;

/** 奏了：手持/穿戴时提升移动速度（每级 +10%） */
public class ZouLeZouLeModifier extends Modifier implements AttributesModifierHook {

    private static final UUID SPEED_UUID = UUID.fromString("e5f6a7b8-c9d0-1234-efab-345678901234");

    public ZouLeZouLeModifier() {}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ATTRIBUTES);
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Attribute, AttributeModifier> consumer) {
        // MULTIPLY_TOTAL：0.1 即 +10%，按等级线性叠加
        consumer.accept(Attributes.MOVEMENT_SPEED,
                new AttributeModifier(SPEED_UUID, "zoulezoule_speed", modifier.getLevel() * 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
