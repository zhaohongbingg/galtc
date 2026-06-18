package com.galtc2.modifiers;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class MeatbunModifier extends Modifier implements InventoryTickModifierHook {

    public MeatbunModifier() {}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity, int slot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        
        if (!isCorrectSlot) return;
        
        FoodData foodData = player.getFoodData();
        boolean isPlayerHealingWithSaturation =
                player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)
                && player.isHurt()
                && foodData.getSaturationLevel() > 0.0;
        if (!isPlayerHealingWithSaturation) {
            foodData.setExhaustion(0);
        }
    }
}
