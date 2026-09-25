package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.init.JerotesMobEffectTags;
import com.jerotes.jerotes.init.JerotesMobEffects;
import com.jerotes.jerotes.util.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 紫沙鬼婆之眼(purple_sand_hag_eye)
 * 复刻 J村：在鬼婆之眼(夜视/清失明)基础上，还能看穿目标隐身——
 * 佩戴/手持时，会移除正前方 32 格内目标的隐身/潜行/真隐效果。
 * 占用能力槽。
 */
public class PurpleSandHagEyeModifier extends HagEyeModifier {

    private static final double RANGE = 32.0;

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        super.onInventoryTick(tool, modifier, world, holder, itemSlot, isSelected, isCorrectSlot, stack);
        if (world.isClientSide || !isCorrectSlot) {
            return;
        }
        // 潜行时获得真实视觉
        if (holder.isShiftKeyDown()) {
            holder.addEffect(new MobEffectInstance(JerotesMobEffects.TRUESIGHT.get(), 25, 1, false, false), holder);
        }
        if (holder instanceof Player player) {
            Entity target = Main.getTargetedEntity(player, RANGE);
            if (target instanceof LivingEntity living && living != holder) {
                removeInvisibility(living);
            }
        }
    }

    /** 移除目标的各类隐身效果 */
    private static void removeInvisibility(LivingEntity living) {
        living.removeEffect(MobEffects.INVISIBILITY);
        living.removeEffect(JerotesMobEffects.CLOAKING.get());
        living.removeEffect(JerotesMobEffects.TRUE_INVISIBLE.get());
        living.level().registryAccess().registryOrThrow(Registries.MOB_EFFECT)
                .getTagOrEmpty(JerotesMobEffectTags.TRUE_INVISIBLE)
                .forEach(holder -> {
                    MobEffect effect = holder.value();
                    if (living.hasEffect(effect)) {
                        living.removeEffect(effect);
                    }
                });
    }
}
