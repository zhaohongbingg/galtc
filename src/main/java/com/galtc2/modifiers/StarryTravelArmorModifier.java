package com.galtc2.modifiers;

import com.galtc2.GaltcConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

public class StarryTravelArmorModifier extends Modifier implements InventoryTickModifierHook {

    private static final UUID ARMOR_UUID = UUID.fromString("12345678-1234-1234-1234-123456789012");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("23456789-2345-2345-2345-234567890123");

    public StarryTravelArmorModifier() {}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, net.minecraft.world.level.Level level, LivingEntity entity, int slot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide()) return;
        if (!isCorrectSlot) return;
        if (entity.tickCount % 20 != 0) return; // 每秒更新一次

        int armorBonus = StarryTravelModifier.getArmorBonus(entity);

        AttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        AttributeInstance toughnessAttr = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);

        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_UUID);
            if (armorBonus > 0) {
                armorAttr.addPermanentModifier(new AttributeModifier(ARMOR_UUID, "starry_travel_armor", armorBonus, AttributeModifier.Operation.ADDITION));
            }
        }

        if (toughnessAttr != null) {
            toughnessAttr.removeModifier(TOUGHNESS_UUID);
            if (armorBonus > 0) {
                toughnessAttr.addPermanentModifier(new AttributeModifier(TOUGHNESS_UUID, "starry_travel_toughness", armorBonus, AttributeModifier.Operation.ADDITION));
            }
        }
    }
}
