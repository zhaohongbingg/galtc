package com.galtc2.modifiers;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 被妹妹治愈了 - 受到伤害后，恢复3/4的血量
 */
public class HealedBySister extends Modifier implements OnAttackedModifierHook {

    public HealedBySister() {}

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        // 受到伤害后恢复3/4的血量
        LivingEntity entity = context.getEntity();
        if (entity != null && !entity.level().isClientSide) {
            float healAmount = amount * 0.75f;
            entity.heal(healAmount);
        }
    }
}
