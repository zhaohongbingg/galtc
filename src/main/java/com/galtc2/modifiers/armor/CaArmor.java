package com.galtc2.modifiers.armor;

import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

/**
 * CA（护甲侧）：获得「幸运值 × 幸运值」点护甲。
 * <p>
 * 工具侧 {@code galtc2:valuation} 用幸运值赌伤害倍率，这条把同一个幸运属性换算成护甲：
 * 幸运会被夹到 [-10, 10]，因此加成区间是 0 ~ 100。
 */
public class CaArmor extends BaseModifiers {

    private static final UUID ARMOR_UUID = UUID.fromString("1c0a9d64-5a41-4c1f-9f27-3a6a1d2b8e01");
    private static final String ARMOR_NAME = "galtc2:ca_armor";
    private static final double MAX_LUCK = 10.0D;

    public CaArmor() {
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
        // 四件护甲各回调一次，只在第一件上结算，避免加成叠加四份
        if (!ArmorAttributeHelper.isFirstPiece(player, this, itemStack, itemSlot)) {
            return;
        }
        double luck = player.getAttributeValue(Attributes.LUCK);
        luck = Math.max(-MAX_LUCK, Math.min(MAX_LUCK, luck));
        ArmorAttributeHelper.setArmor(player, ARMOR_UUID, ARMOR_NAME, luck * luck);
    }

    @Override
    public void modifierOnUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (context.getEntity() instanceof Player player) {
            ArmorAttributeHelper.clearArmor(player, ARMOR_UUID);
        }
    }
}
