package com.galtc2.modifiers;

import com.galtc2.GaltcConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

public class HeartWallModifier extends Modifier implements InventoryTickModifierHook, TooltipModifierHook {

    private static final ResourceLocation SHIELD_CURRENT = new ResourceLocation("galtc2", "heartwall_current");
    private static final ResourceLocation SHIELD_MAX = new ResourceLocation("galtc2", "heartwall_max");

    public HeartWallModifier() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK, ModifierHooks.TOOLTIP);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, net.minecraft.world.level.Level level, LivingEntity entity, int slot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide()) return;
        if (!isCorrectSlot) return;

        int level_val = modifier.getLevel();
        float maxShield = level_val * GaltcConfig.COMMON.HEARTWALL_SHIELD_PER_LEVEL.get();

        CompoundTag tag = itemStack.getOrCreateTag();
        if (!tag.contains("heartwall_max")) {
            tag.putFloat("heartwall_max", maxShield);
            tag.putFloat("heartwall_current", maxShield);
        }

        float current = tag.getFloat("heartwall_current");
        if (current < maxShield && entity.tickCount % 20 == 0) {
            tag.putFloat("heartwall_current", Math.min(current + 1, maxShield));
            if (tag.getBoolean("heartwall_broken") && current + 1 >= maxShield) {
                tag.putBoolean("heartwall_broken", false);
            }
        }

        tool.getPersistentData().putFloat(SHIELD_CURRENT, tag.getFloat("heartwall_current"));
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        int level = modifier.getLevel();
        float maxShield = level * GaltcConfig.COMMON.HEARTWALL_SHIELD_PER_LEVEL.get();
        float current = tool.getPersistentData().getFloat(SHIELD_CURRENT);
        if (current == 0) current = maxShield;
        tooltip.add(Component.literal("§b心之壁: " + (int) current + " / " + (int) maxShield));
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        for (ItemStack armor : entity.getArmorSlots()) {
            if (armor.isEmpty()) continue;
            CompoundTag tag = armor.getTag();
            if (tag == null || !tag.contains("heartwall_current")) continue;

            float shield = tag.getFloat("heartwall_current");
            if (shield <= 0) continue;

            float damage = event.getAmount();
            if (shield >= damage) {
                float remaining = shield - damage;
                tag.putFloat("heartwall_current", remaining);
                event.setAmount(0);
                if (entity instanceof Player player) {
                    player.sendSystemMessage(Component.literal("§b心之壁: " + (int) remaining + " / " + (int) tag.getFloat("heartwall_max")));
                }
            } else {
                tag.putFloat("heartwall_current", 0);
                event.setAmount(damage - shield);

                if (entity instanceof Player player) {
                    player.sendSystemMessage(Component.literal("§c§l心之壁已被打破！"));
                    tag.putBoolean("heartwall_broken", true);
                }
            }
            break;
        }
    }
}