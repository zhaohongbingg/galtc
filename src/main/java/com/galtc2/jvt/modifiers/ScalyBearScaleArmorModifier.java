package com.galtc2.jvt.modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 鳞熊鳞片（护甲侧）：来自中立生物的伤害减少 20%。
 * <p>
 * 工具侧 {@link ScalyBearScaleModifier} 是对「友好生物」增伤，这里取对称的防御向：
 * 攻击者不是敌对生物（{@link Enemy}）时减伤，覆盖中立与友好两类。
 */
public class ScalyBearScaleArmorModifier extends Modifier implements ModifyDamageModifierHook {

    private static final float REDUCTION_PER_LEVEL = 0.2F;

    public ScalyBearScaleArmorModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MODIFY_HURT);
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0.0F) {
            return amount;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)) {
            return amount;
        }
        // 敌对生物不在「中立」范围内
        if (attacker instanceof Enemy) {
            return amount;
        }
        float reduction = Math.min(1.0F, REDUCTION_PER_LEVEL * modifier.getLevel());
        return amount * (1.0F - reduction);
    }
}
