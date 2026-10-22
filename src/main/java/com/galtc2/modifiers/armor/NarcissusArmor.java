package com.galtc2.modifiers.armor;

import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 水仙（护甲侧）：免疫低于自身最大生命值一半的伤害。
 * <p>
 * 工具侧 {@code galtc2:narcissus} 是额外造成目标最大生命 15% 的伤害，这条是同一套数值的镜像：
 * 只要这一下伤害低于自己最大生命的一半，就直接抹掉；高血量下的小额多段伤害全部免疫，
 * 大伤害照吃。判定用的是「护甲减伤之后」的数值，所以实际阈值会比面板最大生命的一半宽一点。
 */
public class NarcissusArmor extends BaseModifiers {

    /** 免疫阈值：自身最大生命 × 该系数 */
    private static final float IMMUNE_HEALTH_FACTOR = 0.5F;

    public NarcissusArmor() {
    }

    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public float onModifyTakeDamage(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity holder = context.getEntity();
        if (holder == null || amount <= 0.0F) {
            return amount;
        }
        if (amount < holder.getMaxHealth() * IMMUNE_HEALTH_FACTOR) {
            return 0.0F;
        }
        return amount;
    }
}
