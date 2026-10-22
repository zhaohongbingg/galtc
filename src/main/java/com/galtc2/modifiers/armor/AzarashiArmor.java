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
 * 海豹（护甲侧）：面板防御数值超过 50 时翻 3 倍。
 * <p>
 * 工具侧 {@code galtc2:azarashi} 是「攻击力超过阈值伤害 ×3」，这里同样的判定换成护甲：
 * 读到的是<b>四件护甲合起来的面板值</b>（含其它特性 / 附魔 / 强化的加成），
 * 面板 &gt; 50 时额外补上「面板 × 2」，等效总护甲 ×3。
 */
public class AzarashiArmor extends BaseModifiers {

    private static final UUID ARMOR_UUID = UUID.fromString("2d7b1e35-8c64-4a92-b0d3-5e8f4c1a6b02");
    private static final String ARMOR_NAME = "galtc2:azarashi_armor";
    private static final int THRESHOLD = 50;
    private static final double EXTRA_MULTIPLIER = 2.0D;

    public AzarashiArmor() {
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
        // 先扣掉自己加的那部分，避免「护甲越高→加成越高→护甲更高」的自反馈
        int base = ArmorAttributeHelper.baseArmor(player, ARMOR_UUID);
        double bonus = base > THRESHOLD ? base * EXTRA_MULTIPLIER : 0.0D;
        ArmorAttributeHelper.setArmor(player, ARMOR_UUID, ARMOR_NAME, bonus);
    }

    @Override
    public void modifierOnUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (context.getEntity() instanceof Player player) {
            ArmorAttributeHelper.clearArmor(player, ARMOR_UUID);
        }
    }
}
