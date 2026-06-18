package com.galtc2.modifiers;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class SpoiledByRichGirl extends Modifier implements EquipmentChangeModifierHook {

    private static final int EFFECT_DURATION = Integer.MAX_VALUE;

    public SpoiledByRichGirl() {}

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.EQUIPMENT_CHANGE);
    }

    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        LivingEntity entity = context.getEntity();
        if (entity instanceof Player player && !entity.level().isClientSide()) {
            int level = modifier.getLevel();
            int amplifier = level + 1;
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, EFFECT_DURATION, amplifier, false, false, true));
        }
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        LivingEntity entity = context.getEntity();
        if (entity instanceof Player player && !entity.level().isClientSide()) {
            player.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
        }
    }
}
