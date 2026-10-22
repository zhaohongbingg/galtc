package com.galtc2.modifiers.armor;

import com.galtc2.GaltcConfig;
import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

/**
 * 所长（护甲侧）：按物品栏里的钱财数量提升防御。
 * <p>
 * 工具侧 {@code galtc2:shochou} 打人掉钱，这条把钱变成护甲：只统计配置里指定的钱币物品
 * （{@code shochou_money_item}），主背包 + 副手一起算，每个 +0.25 护甲，封顶 +20。
 */
public class ShochouArmor extends BaseModifiers {

    private static final UUID ARMOR_UUID = UUID.fromString("5a0e4b68-bf97-4d25-a136-8c1d7f4e9a05");
    private static final String ARMOR_NAME = "galtc2:shochou_armor";
    private static final double ARMOR_PER_ITEM = 0.25D;
    private static final double MAX_BONUS = 20.0D;

    public ShochouArmor() {
    }

    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide() || !isCorrectSlot || !(holder instanceof Player player)) {
            return;
        }
        if (!ArmorAttributeHelper.isFirstPiece(player, this, itemStack, itemSlot)) {
            return;
        }
        int count = countMoney(player);
        double bonus = Math.min(MAX_BONUS, count * ARMOR_PER_ITEM);
        ArmorAttributeHelper.setArmor(player, ARMOR_UUID, ARMOR_NAME, bonus);
    }

    @Override
    public void modifierOnUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (context.getEntity() instanceof Player player) {
            ArmorAttributeHelper.clearArmor(player, ARMOR_UUID);
        }
    }

    private int countMoney(Player player) {
        ResourceLocation location = ResourceLocation.tryParse(GaltcConfig.COMMON.SHOCHOU_MONEY_ITEM.get());
        if (location == null) {
            return 0;
        }
        Item money = ForgeRegistries.ITEMS.getValue(location);
        if (money == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(money)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.is(money)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
