package com.galtc2.compat.meetyourfight.modifiers;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;

/**
 * 迎战 联动特性共用的小工具。
 * <p>
 * 护甲钩子（{@code MODIFY_HURT} / {@code ON_ATTACKED}）对「每件带该特性的护甲」各调一次，
 * 所以减伤/免伤/给 buff 这类效果都要先确定"由哪一件护甲负责结算"，否则穿 4 件就会结算 4 次。
 * 这里统一按固定槽位顺序（脚→腿→胸→头）取第一件带该强化的护甲。
 */
public final class MeetYourFightHelper {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private MeetYourFightHelper() {
    }

    /** 穿戴了该强化的第一件护甲槽位；一件都没有时返回 null */
    @Nullable
    public static EquipmentSlot firstArmorSlot(EquipmentContext context, Modifier modifier) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(modifier) > 0) {
                return slot;
            }
        }
        return null;
    }

    /** 穿戴了该强化的护甲件数 */
    public static int armorCount(EquipmentContext context, Modifier modifier) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(modifier) > 0) {
                count++;
            }
        }
        return count;
    }

    /** 取实体的幸运值（原版 {@code Attributes.LUCK}，和迎战 里的 {@code LivingEntity#getLuck()} 是同一个值）。 */
    public static float luckOf(LivingEntity entity) {
        return (float) entity.getAttributeValue(Attributes.LUCK);
    }

    /**
     * 迎战 的「幸运修正概率」公式（原样照抄 {@code lykrast.meetyourfight.misc.EventHandler}
     * 与 {@code item.CocktailCutlass}）：
     * <pre>
     *   luck &gt;= 0 :  (1 + 正分子系数 * L) / ((1 / P) + 正分母系数 * L)
     *   luck &lt;  0 :  1 / ((1 / P) - 负系数 * L)
     * </pre>
     * L = 0 时结果就是基础概率 P，正负分支不对称（迎战 里就是这么写的，没有做平滑）。
     *
     * @param baseChance          基础触发概率 P
     * @param luck                幸运值 L
     * @param positiveNumerator   正幸运分支的分子系数
     * @param positiveDenominator 正幸运分支的分母系数
     * @param negativeCoefficient 负幸运分支的系数
     */
    public static double luckChance(double baseChance, float luck,
                                    double positiveNumerator, double positiveDenominator, double negativeCoefficient) {
        if (baseChance <= 0) {
            return 0;
        }
        if (luck >= 0) {
            return (1.0 + positiveNumerator * luck) / ((1.0 / baseChance) + positiveDenominator * luck);
        }
        return 1.0 / ((1.0 / baseChance) - negativeCoefficient * luck);
    }
}
