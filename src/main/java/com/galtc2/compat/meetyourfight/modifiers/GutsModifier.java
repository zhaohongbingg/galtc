package com.galtc2.compat.meetyourfight.modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 根性 (guts) —— 迎战 联动特性：藓牙 材料的防御侧特性。
 * <p>
 * 对应迎战 里用藓牙 做的"囚笼之心"（{@code cagedHeartTreshold} / {@code cagedHeartMultiplier}）：
 * 单次伤害超过最大生命值 50% 时，整段伤害只吃 50%（也就是"减免超过最大生命一半的伤害，减少 50%"）。
 * <p>
 * 护甲侧只由第一件带此特性的护甲结算：4 件全穿也只按一次算，不然会变成 1/16 伤害。
 */
public class GutsModifier extends Modifier implements ModifyDamageModifierHook {

    /** 触发阈值：最大生命值的比例 */
    private static final float THRESHOLD = 0.5f;
    /** 触发后的伤害倍率（0.5 = 减少 50%） */
    private static final float MULTIPLIER = 0.5f;

    public GutsModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MODIFY_HURT);
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                                   EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0) {
            return amount;
        }
        if (slotType != MeetYourFightHelper.firstArmorSlot(context, this)) {
            return amount;
        }
        LivingEntity entity = context.getEntity();
        if (entity == null) {
            return amount;
        }
        if (amount > entity.getMaxHealth() * THRESHOLD) {
            return amount * MULTIPLIER;
        }
        return amount;
    }
}
