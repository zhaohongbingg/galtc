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
 * 复刻 J村 莫厄制式套，但不再区分部位：
 * 任意一件带有该特性的护甲即可获得全部效果
 * - 夜视/真视，水中水下呼吸；
 * - 力量；
 * - 潜行隐身、疾跑速度与跳跃提升；
 * - 窒息 -8、非穿透 -4(魔法命中减速攻击者)、摔落免疫；
 * - 额外 -30% 伤害。
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
        // 不限制部位：身上任意一件带该特性的护甲就触发全部效果
        if (countPieces(holder) <= 0) {
            return;
        }
        holder.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 245, 0, false, false), holder);
        holder.addEffect(new MobEffectInstance(JerotesMobEffects.TRUESIGHT.get(), 245, 0, false, false), holder);
        if (holder.isInWater()) {
            holder.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 245, 0, false, false), holder);
        }
        holder.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 65, 0, false, false), holder);
        if (holder.isShiftKeyDown()) {
            holder.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 65, 0, false, false), holder);
        }
        if (holder.isSprinting()) {
            holder.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 1, false, false), holder);
            holder.addEffect(new MobEffectInstance(MobEffects.JUMP, 25, 1, false, false), holder);
        }
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0) {
            return amount;
        }
        // 不限制部位：任意一件带该特性的护甲即可生效
        EquipmentSlot firstSlot = firstPiece(context);
        if (firstSlot == null) {
            return amount;
        }
        // 每个护甲槽都会回调一次，只在第一件上结算，避免重复叠加
        if (slotType != firstSlot) {
            return amount;
        }
        LivingEntity entity = context.getEntity();
        if (source.is(DamageTypeTags.IS_FALL)) {
            return 0;
        }
        float result = amount;
        if (source.is(DamageTypes.IN_WALL)) {
            result -= 8.0f;
        }
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            result -= 4.0f;
            if (AttackFind.MagicResistance(source) && source.getEntity() instanceof LivingEntity attacker) {
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 240, 1, false, false), entity);
            }
        }
        result -= amount * 0.3f;
        if (result < 0.0f && amount >= 0.0f) {
            result = 0.0f;
        }
        return result;
    }

    /** 首个带有该特性的护甲槽，没有则返回 null */
    private EquipmentSlot firstPiece(EquipmentContext context) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(this) > 0) {
                return slot;
            }
        }
        return null;
    }

    /** 统计身上带该特性的护甲件数 */
    private int countPieces(LivingEntity holder) {
        int pieces = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (has(holder, slot)) {
                pieces++;
            }
        }
        return pieces;
    }

    private boolean has(LivingEntity holder, EquipmentSlot slot) {
        return ModifierUtil.getModifierLevel(holder.getItemBySlot(slot), this.getId()) > 0;
    }
}
