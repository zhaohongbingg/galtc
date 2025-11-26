package com.galtc2.modifiers;

import com.galtc2.GaltcConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.*;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public class AIArmor extends BaseModifiers {

    private static int counts = 0;

    private static final String hitcount = "hitcount";
    private static final ResourceLocation HIT_COUNT = new ResourceLocation("galtc2", "hit_count");
    private static final int HIT_THRESHOLD = GaltcConfig.COMMON.HIT_THRESHOLD.get();
    private static final ResourceLocation ARMOR_BONUS =
            new ResourceLocation("galtc2", "armor_bonus");

    @Override
    public void onAttacked(IToolStackView tool,
                           ModifierEntry modifier,
                           EquipmentContext context,
                           EquipmentSlot slotType,
                           DamageSource source,
                           float amount,
                           boolean isDirectDamage) {

        ModDataNBT data = tool.getPersistentData();

        int count = data.getInt(HIT_COUNT) + 1;

        if (count >= HIT_THRESHOLD) {

            int armorBonus = data.getInt(ARMOR_BONUS);
            data.putInt(ARMOR_BONUS, armorBonus + 1);

            count -= HIT_THRESHOLD;
        }

        data.putInt(HIT_COUNT, count);

        // ❗ 必须刷新，让匠魂重算
        if (context.getEntity() instanceof Player player && !player.level().isClientSide) {
            ItemStack stack = player.getItemBySlot(slotType);
            if (!stack.isEmpty()) {
                player.setItemSlot(slotType, stack.copy());
            }
        }

        super.onAttacked(tool, modifier, context, slotType, source, amount, isDirectDamage);
    }
    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {

        IModDataView data = context.getPersistentData();

        int armorBonus = data.getInt(new ResourceLocation("galtc2", "armor_bonus"));
        if (armorBonus > 0) {
            ToolStats.ARMOR.add(builder, armorBonus);
            ToolStats.ARMOR_TOUGHNESS.add(builder, armorBonus);
        }
    }


}






