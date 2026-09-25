package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.init.JerotesMobEffects;
import com.jerotes.jerotes.util.AttackFind;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 精炼莫厄·莫厄制式(meror_standard)
 * 复刻 J村 莫厄制式套：
 * - 头盔：夜视/真视，水中水下呼吸；
 * - 胸甲：力量；
 * - 护腿：潜行隐身、疾跑速度与跳跃提升；
 * - 头盔 IN_WALL -8、胸甲非穿透 -4(魔法命中减速攻击者)、靴子摔落免疫；
 * - 全套 -30% 伤害。
 */
public class MerorStandardModifier extends Modifier implements ModifyDamageModifierHook, InventoryTickModifierHook {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MODIFY_HURT, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide || !isCorrectSlot || !holder.isAlive()) {
            return;
        }
        if (has(holder, EquipmentSlot.HEAD)) {
            holder.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 245, 0, false, false), holder);
            holder.addEffect(new MobEffectInstance(JerotesMobEffects.TRUESIGHT.get(), 245, 0, false, false), holder);
            if (holder.isInWater()) {
                holder.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 245, 0, false, false), holder);
            }
        }
        if (has(holder, EquipmentSlot.CHEST)) {
            holder.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 65, 0, false, false), holder);
        }
        if (has(holder, EquipmentSlot.LEGS)) {
            if (holder.isShiftKeyDown()) {
                holder.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 65, 0, false, false), holder);
            }
            if (holder.isSprinting()) {
                holder.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 1, false, false), holder);
                holder.addEffect(new MobEffectInstance(MobEffects.JUMP, 25, 1, false, false), holder);
            }
        }
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0) {
            return amount;
        }
        EquipmentSlot firstSlot = null;
        int pieces = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(this) > 0) {
                pieces++;
                if (firstSlot == null) {
                    firstSlot = slot;
                }
            }
        }
        if (firstSlot == null || slotType != firstSlot) {
            return amount;
        }
        LivingEntity entity = context.getEntity();
        // 靴子摔落免疫
        if (has(entity, EquipmentSlot.FEET) && source.is(DamageTypeTags.IS_FALL)) {
            return 0;
        }
        float result = amount;
        if (has(entity, EquipmentSlot.HEAD) && source.is(DamageTypes.IN_WALL)) {
            result -= 8.0f;
        }
        if (has(entity, EquipmentSlot.CHEST) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            result -= 4.0f;
            if (AttackFind.MagicResistance(source) && source.getEntity() instanceof LivingEntity attacker) {
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 240, 1, false, false), entity);
            }
        }
        if (pieces >= 4) {
            result -= amount * 0.3f;
        }
        if (result < 0.0f && amount >= 0.0f) {
            result = 0.0f;
        }
        return result;
    }

    private boolean has(LivingEntity holder, EquipmentSlot slot) {
        return ModifierUtil.getModifierLevel(holder.getItemBySlot(slot), this.getId()) > 0;
    }
}
