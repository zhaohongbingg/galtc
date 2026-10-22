package com.galtc2.modifiers.armor;

import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

/**
 * 夜羊（护甲侧）：防御数值低于 18 时补足差额。
 * <p>
 * 工具侧 {@code galtc2:nighttimesheep} 是「攻击力越低加成越高」，对称地这里把低护甲补到 18：
 * 基础护甲 5 就 +13，基础护甲 18 及以上不再加成。
 */
public class NightTimeSheepArmor extends BaseModifiers {

    private static final UUID ARMOR_UUID = UUID.fromString("3e8c2f46-9d75-4b03-8f14-6a9b5d2c7e03");
    private static final String ARMOR_NAME = "galtc2:nighttimesheep_armor";
    private static final int THRESHOLD = 18;

    public NightTimeSheepArmor() {
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
        int base = ArmorAttributeHelper.baseArmor(player, ARMOR_UUID);
        double bonus = base < THRESHOLD ? THRESHOLD - base : 0.0D;
        ArmorAttributeHelper.setArmor(player, ARMOR_UUID, ARMOR_NAME, bonus);
    }

    @Override
    public void modifierOnUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (context.getEntity() instanceof Player player) {
            ArmorAttributeHelper.clearArmor(player, ARMOR_UUID);
        }
    }
}
