package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.init.JerotesMobEffects;
import com.jerotes.jerotes.util.AttackFind;
import com.jerotes.jerotes.util.EntityAndItemFind;
import com.jerotes.jerotes.util.Main;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * 镇山(mountain_realm) 材料特性。
 * 复刻 J村 镇山套：
 * - 头盔：使直视的目标获得震慑(ABACK)；
 * - 胸甲：近战攻击把小型生物击飞；
 * - 护腿：疾跑时给附近生物虚弱；
 * - 全套：清除自身缓慢与虚弱。
 */
public class MountainRealmModifier extends Modifier implements InventoryTickModifierHook, MeleeHitModifierHook {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide || !isCorrectSlot || !holder.isAlive()) {
            return;
        }
        if (countPieces(holder) >= 4) {
            holder.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            holder.removeEffect(MobEffects.WEAKNESS);
        }
        if (has(holder, EquipmentSlot.HEAD) && holder instanceof Player player) {
            Entity target = Main.getTargetedEntity(player, 32.0);
            if (target instanceof LivingEntity living && !AttackFind.FindCanNotAttack(player, living)) {
                living.addEffect(new MobEffectInstance(JerotesMobEffects.ABACK.get(), 5, 0), player);
            }
        }
        if (has(holder, EquipmentSlot.LEGS) && holder.isSprinting() && holder.tickCount % 5 == 0) {
            List<LivingEntity> nearby = holder.level().getEntitiesOfClass(LivingEntity.class, holder.getBoundingBox().inflate(6.0));
            for (LivingEntity living : nearby) {
                if (living == holder || !living.isAlive() || AttackFind.FindCanNotAttack(holder, living)) {
                    continue;
                }
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1), holder);
            }
        }
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target == null || !target.isAlive() || !has(attacker, EquipmentSlot.CHEST)) {
            return;
        }
        if (EntityAndItemFind.isNoSpecialKnockback(target.getType())) {
            return;
        }
        if (!Main.mobSizeSmall(target) && !Main.mobSizeMedium(target) && !Main.mobSizeLarge(target)) {
            return;
        }
        double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double power = Math.max(0.0, 1.0 - resistance) + 0.3;
        target.setOnGround(false);
        target.setDeltaMovement(target.getDeltaMovement().add(
                -((attacker.getX() - target.getX()) * power / 3.0) * 0.8666667,
                -((attacker.getY() - target.getY()) * 1.5 * power) * 0.8666667,
                -((attacker.getZ() - target.getZ()) * power / 3.0) * 0.8666667));
    }

    private boolean has(LivingEntity holder, EquipmentSlot slot) {
        return ModifierUtil.getModifierLevel(holder.getItemBySlot(slot), this.getId()) > 0;
    }

    private int countPieces(LivingEntity holder) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (has(holder, slot)) {
                count++;
            }
        }
        return count;
    }
}
