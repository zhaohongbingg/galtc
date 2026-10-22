package com.galtc2.modifiers.armor;

import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 零一（护甲侧）：延长受击后的无敌帧。
 * <p>
 * 工具侧 {@code galtc2:eightstrike} 是「命中后清零目标无敌帧」，这条是反过来的自保：
 * 挨打后把自身的 {@code invulnerableTime} 顶到比原版 20 tick 更长，挡掉连续多段伤害。
 */
public class ZeroOneArmor extends BaseModifiers {

    /** 原版受击后固定的无敌时间 */
    private static final int VANILLA_INVULNERABLE = 20;
    /** 每级额外延长 */
    private static final int BONUS_TICKS_PER_LEVEL = 10;

    public ZeroOneArmor() {
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
        int expected = VANILLA_INVULNERABLE + BONUS_TICKS_PER_LEVEL * modifier.getLevel();
        if (holder.invulnerableTime < expected) {
            holder.invulnerableTime = expected;
        }
    }
}
