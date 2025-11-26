package com.galtc2.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.common.TinkerTags.Items;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.helper.TooltipBuilder;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Consumer;

public class TinkerUtils {
    public static boolean checkTool(ItemStack stack) {
        if (!stack.isEmpty() && stack.is(Items.MODIFIABLE)) {
            if (stack.getItem() instanceof IModifiable modifiable) {
                return modifiable.getToolDefinition() != ToolDefinition.EMPTY;
            }
        }
        return false;
    }
    @Nullable
    public static ToolStack getReciprocalSlotArmor(LivingEntity entity) {
        for(int i = 3; i > 0; --i) {
            EquipmentSlot slot = EquipmentSlot.byTypeAndIndex(Type.ARMOR, i);
            ItemStack stack = entity.getItemBySlot(slot);
            if (checkTool(stack)) {
                return ToolStack.from(stack);
            }
        }
        return null;
    }
    public static int getTraitCountWithModifier(IToolContext context, ModifierId modifier) {
        int count = 0;
        for(int i = 0; i < context.getMaterials().size(); ++i) {
            MaterialVariant material = context.getMaterial(i);
            for(ModifierEntry trait : MaterialRegistry.getInstance().getDefaultTraits(material.getId())) {
                if (trait.getId().equals(modifier)) ++count;
            }
        }
        return count;
    }

    public static float getStatValue(ModifierStatsBuilder builder, INumericToolStat<?> stat, boolean withMultiplier) {
        float value = builder.getStat(stat).floatValue();
        return withMultiplier ? value * builder.getMultiplier(stat) : value;
    }

    public static List<ModifierEntry> getPartTraits(ItemStack partItem) {
        if (partItem.getItem() instanceof IToolPart part) {
            return MaterialRegistry.getInstance().getTraits(part.getMaterial(partItem).getId(), part.getStatType());
        } else {
            return List.of();
        }
    }

    public static void addTooToTab(CreativeModeTab.Output output, IModifiable tool) {
        Consumer<ItemStack> cons = output::accept;
        ToolBuildHandler.addVariants(cons, tool, "");
    }

    public static void addPartToTab(CreativeModeTab.Output output, IMaterialItem part) {
        Consumer<ItemStack> cons = output::accept;
        part.addVariants(cons, "");
    }



    public static void addPerStatInfo(TooltipBuilder builder, IToolStackView tool, FloatToolStat stat) {
        builder.add(Component.translatable(stat.getTranslationKey()).append(Component.translatable(Util.PERCENT_FORMAT.format(tool.getStats().get(stat)))
                .withStyle((style) -> style.withColor(stat.getColor()))));
    }





    public static ItemStack getRangedTool(LivingEntity entity) {
        for(InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = entity.getItemInHand(hand);
            if (stack.getItem() instanceof IModifiable && stack.is(Items.RANGED)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }



    public static boolean matchesPart(IToolStackView tool, IToolPart part) {
        return ToolPartsHook.parts(tool.getDefinition()).stream().anyMatch((p) -> p.getStatType().equals(part.getStatType()));
    }
}

