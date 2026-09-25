package com.galtc2.jvt.modifiers;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 鬼婆之眼(hag_eye)
 * 复刻 J村：佩戴/手持时持续获得夜视，并清除自身失明/黑暗。
 * 占用能力槽。
 */
public class HagEyeModifier extends Modifier implements InventoryTickModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide || !isCorrectSlot || !holder.isAlive()) {
            return;
        }
        applyEyeSight(holder);
    }

    /** 夜视 + 清除失明/黑暗 */
    protected void applyEyeSight(LivingEntity holder) {
        holder.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 255, 0, false, false), holder);
        holder.removeEffect(MobEffects.BLINDNESS);
        holder.removeEffect(MobEffects.DARKNESS);
    }
}
