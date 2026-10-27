package com.galtc2.modifiers;

import com.galtc2.Register.galtc2Effects;
import com.galtc2.content.effect.sakura;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 樱花印记（护甲侧）—— 生命的备件材料的防御版特性。
 * <p>
 * 工具侧 {@link oumunnbyou} 是「命中时给目标叠樱花印记」；
 * 护甲侧反过来：<b>被打时把樱花印记叠给攻击者</b>，让「病的传染」变成防御手段。
 * <p>
 * 走 {@code ON_ATTACKED} 钩子（护甲被攻击时触发），而不是 {@code INVENTORY_TICK}
 * —— 后者只对玩家背包里的物品调用，女仆/怪物穿护甲时不会 tick。
 */
public class OumnnbyouArmorModifier extends BaseModifiers {

    /** 印记持续时间（tick） */
    private static final int DURATION = 3000;
    /** 印记最大层数 */
    private static final int MAX_AMPLIFIER = 4;

    public OumnnbyouArmorModifier() {
    }

    @Override
    public void modifierOnAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                                   EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity holder = context.getEntity();
        if (holder == null || holder.level().isClientSide()) {
            return;
        }
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        if (attacker == null || attacker == holder || !attacker.isAlive()) {
            return;
        }
        MobEffectInstance old = attacker.getEffect(galtc2Effects.sakura.get());
        int amplifier = Math.min((old == null ? 0 : old.getAmplifier() + 1), MAX_AMPLIFIER);
        attacker.addEffect(new MobEffectInstance(galtc2Effects.sakura.get(), DURATION, amplifier,
                false, true, true));
    }
}
