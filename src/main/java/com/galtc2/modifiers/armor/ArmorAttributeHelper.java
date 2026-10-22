package com.galtc2.modifiers.armor;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;

import java.util.UUID;

/**
 * 防御特性共用的护甲属性工具。
 * <p>
 * 匠魂的 {@code addAttributes} 钩子拿不到穿戴者（只有工具和槽位），所以「按玩家血量 / 幸运 / 背包
 * 实时变化」的护甲加成统一在 {@code INVENTORY_TICK} 里挂到实体身上，用固定的 UUID 每 tick 覆盖一次。
 * 用 transient modifier：不写进玩家存档，卸下 / 死亡后自然消失。
 */
public final class ArmorAttributeHelper {

    public static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private ArmorAttributeHelper() {
    }

    /** 覆盖式设置额外护甲值（0 或负数表示移除） */
    public static void setArmor(LivingEntity entity, UUID uuid, String name, double amount) {
        AttributeInstance instance = entity.getAttribute(Attributes.ARMOR);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(uuid);
        if (current != null) {
            if (Math.abs(current.getAmount() - amount) < 1.0E-4D) {
                return;
            }
            instance.removeModifier(uuid);
        }
        if (amount > 0.0D) {
            instance.addTransientModifier(new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    public static void clearArmor(LivingEntity entity, UUID uuid) {
        AttributeInstance instance = entity.getAttribute(Attributes.ARMOR);
        if (instance != null && instance.getModifier(uuid) != null) {
            instance.removeModifier(uuid);
        }
    }

    /**
     * 「基础护甲」：四件护甲合起来的面板值，排除本特性自己加的那部分。
     * <p>
     * 不能用 {@code getArmorValue() - 自己的加成}：{@code getArmorValue()} 已经在属性上限处截断，
     * 一旦加成把总护甲顶过上限，减完就成了负数 / 小数字，下一 tick 加成回落，护甲再涨回来，
     * 会变成「每 tick 抖一次」的自反馈。这里按修饰器逐项累加（跳过本特性自己的 UUID），
     * 拿到没有被上限截断的基础值，算出来的加成才是稳定的。
     */
    public static int baseArmor(LivingEntity entity, UUID uuid) {
        AttributeInstance instance = entity.getAttribute(Attributes.ARMOR);
        if (instance == null) {
            return 0;
        }
        double addition = instance.getBaseValue();
        double multipliedBase = 0.0D;
        double multipliedTotal = 1.0D;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.getId().equals(uuid)) {
                continue;
            }
            switch (modifier.getOperation()) {
                case ADDITION -> addition += modifier.getAmount();
                case MULTIPLY_BASE -> multipliedBase += modifier.getAmount();
                case MULTIPLY_TOTAL -> multipliedTotal *= 1.0D + modifier.getAmount();
            }
        }
        double value = addition * (1.0D + multipliedBase) * multipliedTotal;
        return Math.max(0, Mth.floor(value));
    }

    /**
     * 是否「第一件」带该特性的护甲。
     * <p>
     * 护甲特性是每件护甲各算一次，四件都穿会重复触发。需要「任意一件即可、不叠加」的加成
     * （护甲值、减伤）都用这个把结算收敛到第一件上。
     */
    public static boolean isFirstPiece(LivingEntity holder, Modifier modifier, ItemStack current) {
        return isFirstPiece(holder, modifier, current, -1);
    }

    /**
     * 同上，额外用背包序号定位当前护甲所在的装备槽。
     * <p>
     * 玩家主背包 36~39 依次是靴 / 腿 / 胸 / 头，按槽位判断能正确区分「两件一模一样的护甲」；
     * 其它实体（没有固定序号）回退到 ItemStack 引用比较。
     */
    public static boolean isFirstPiece(LivingEntity holder, Modifier modifier, ItemStack current, int itemSlot) {
        EquipmentSlot currentSlot = slotOf(holder, itemSlot);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = holder.getItemBySlot(slot);
            if (stack.isEmpty() || ModifierUtil.getModifierLevel(stack, modifier.getId()) <= 0) {
                continue;
            }
            if (currentSlot != null) {
                return slot == currentSlot;
            }
            return stack == current;
        }
        return false;
    }

    /** 背包序号 → 护甲槽，仅玩家有效 */
    private static EquipmentSlot slotOf(LivingEntity holder, int itemSlot) {
        if (!(holder instanceof net.minecraft.world.entity.player.Player)) {
            return null;
        }
        if (itemSlot < 36 || itemSlot > 39) {
            return null;
        }
        return ARMOR_SLOTS[itemSlot - 36];
    }
}
