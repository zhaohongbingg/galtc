package com.galtc2.modifiers.armor;

import com.galtc2.Register.galtc2Sounds;
import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 丛雨丸（护甲侧）：被打时播放 Ciallo 音效。
 * <p>
 * 音效与工具侧 {@code galtc2:ciallo} 共用 {@link galtc2Sounds#CIALLO}，只是触发点从「命中」变成「受击」。
 */
public class CialloArmor extends BaseModifiers {

    public CialloArmor() {
    }

    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public void modifierOnAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity holder = context.getEntity();
        if (holder == null || holder.level().isClientSide()) {
            return;
        }
        holder.level().playSound(null,
                holder.getX(), holder.getY(), holder.getZ(),
                galtc2Sounds.CIALLO.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);
    }
}
