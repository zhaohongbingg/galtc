package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/**
 * 月社妃武器特性：可最讨厌你（Kisaki Hates You）
 *
 * 防御向/惩罚：
 * - 主手持月社妃武器时，副手持非月社妃材质的武器/工具 → 持续消耗副手耐久（每秒 1% 最大耐久）
 */
public class KisakiHatesModifier extends BaseModifiers {

    /** 每秒消耗副手耐久占最大耐久的比例 */
    private static final float CONSUME_PERCENT = 0.01f;

    @Override
    public void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level,
                                        LivingEntity holder, int itemSlot, boolean isSelected,
                                        boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide()) return;
        if (!isSelected) return;      // 仅主手持有时生效
        if (!(holder instanceof Player player)) return;
        if (tool.isBroken()) return;
        if (level.getGameTime() % 20 != 0) return;

        ItemStack offhand = holder.getOffhandItem();
        if (offhand.isEmpty()) return;
        if (KisakiLikesModifier.isKisakiMaterial(offhand)) return; // 同为月社妃材质不消耗
        if (offhand.getItem() instanceof ArmorItem) return;        // 盔甲不属于"武器"

        // TCon 武器/工具：按官方方式消耗耐久
        if (offhand.getItem() instanceof ModifiableItem) {
            IToolStackView offTool = ToolStack.from(offhand);
            if (offTool.isBroken()) return;
            int max = (int) (float) offTool.getStats().get(ToolStats.DURABILITY);
            int consume = Math.max(1, (int) (max * CONSUME_PERCENT));
            ToolDamageUtil.damage(offTool, consume, player, offhand);
            return;
        }

        // 原版可损坏物品（剑/斧/镐等）：直接扣耐久
        if (offhand.isDamageableItem()) {
            int consume = Math.max(1, (int) (offhand.getMaxDamage() * CONSUME_PERCENT));
            offhand.hurtAndBreak(consume, player, e -> {});
        }
    }
}
