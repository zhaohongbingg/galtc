package com.galtc2.modifiers;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 匠魂特性通用的等级读取。
 * <p>
 * "手上必须有带该特性的武器才触发" + "4 件盔甲上的特性等级按 {@link #ARMOR_LEVEL_DIVISOR} 折算加成"
 * 这套规则在腐姬 / 清枫 / 莫尔加那 上是共用的，抽出来避免各写一份。
 * <p>
 * 注意：<b>盔甲上的特性同样会收到匠魂的近战命中钩子</b>（原版盔甲模块 Enderclearance 就是这么用的），
 * 所以判断"手上是不是该特性的武器"必须回到手里那件物品去查，不能信钩子传进来的 modifier。
 */
public final class TraitLevels {

    /** 参与等级加成的盔甲槽位（4 件套） */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    /** 盔甲等级之和要除以这个数再计入总等级（4 件套 ×1 级 → 只算 2 级） */
    public static final int ARMOR_LEVEL_DIVISOR = 2;

    /** 主手/副手上该特性的等级，取较大者；空手或普通物品返回 0 */
    public static int heldLevel(LivingEntity entity, ModifierId id) {
        return Math.max(heldLevel(entity, InteractionHand.MAIN_HAND, id),
                heldLevel(entity, InteractionHand.OFF_HAND, id));
    }

    private static int heldLevel(LivingEntity entity, InteractionHand hand, ModifierId id) {
        ItemStack stack = entity.getItemInHand(hand);
        if (stack.isEmpty() || !ToolStack.isInitialized(stack)) return 0;
        return ToolStack.from(stack).getModifiers().getLevel(id);
    }

    /** 四件盔甲上该特性的等级之和（盔甲不是匠魂工具、或已损坏时不计入） */
    public static int armorLevel(LivingEntity entity, ModifierId id) {
        int total = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !ToolStack.isInitialized(stack)) continue;
            ToolStack tool = ToolStack.from(stack);
            if (tool.isBroken()) continue;
            total += tool.getModifiers().getLevel(id);
        }
        return total;
    }

    /**
     * 武器侧等级：手上没有该特性的武器时返回 0（= 不触发），
     * 否则返回 {@code 手上等级 + 盔甲之和 / ARMOR_LEVEL_DIVISOR}。
     */
    public static int weaponLevel(LivingEntity entity, ModifierId id) {
        int held = heldLevel(entity, id);
        if (held <= 0) return 0;
        return held + armorLevel(entity, id) / ARMOR_LEVEL_DIVISOR;
    }

    private TraitLevels() {
    }
}
