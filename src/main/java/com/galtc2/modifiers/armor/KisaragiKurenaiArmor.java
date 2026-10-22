package com.galtc2.modifiers.armor;

import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Spider;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 月影魅像（护甲侧）：来自蜘蛛类的伤害减免 50%。
 * <p>
 * 工具侧 {@code galtc2:kisaragikurenai} 是对「非蜘蛛」增伤，这条把蜘蛛单独拎出来做减伤，
 * 判定沿用同一套 {@link Spider} / {@link CaveSpider} 类型判断。
 */
public class KisaragiKurenaiArmor extends BaseModifiers {

    private static final float REDUCTION = 0.5F;

    public KisaragiKurenaiArmor() {
    }

    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public float onModifyTakeDamage(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        if (amount <= 0.0F) {
            return amount;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof Spider || attacker instanceof CaveSpider) {
            return amount * (1.0F - REDUCTION);
        }
        return amount;
    }
}
