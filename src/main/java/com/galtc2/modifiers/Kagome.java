package com.galtc2.modifiers;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.function.Predicate;

public class Kagome extends BaseModifiers{
    public  Kagome(){}
    @Override
    public boolean isNoLevels() {
        return true;
    }
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity entity, int index, boolean select, boolean current, ItemStack stack) {
        if (current) {
            Predicate<MobEffectInstance> predicate = (ins) -> ins.getEffect().getCategory().equals(MobEffectCategory.HARMFUL);
            entity.getActiveEffects().removeIf(predicate);
        }

    }


}
