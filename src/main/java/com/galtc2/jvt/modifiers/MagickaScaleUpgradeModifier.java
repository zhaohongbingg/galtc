package com.galtc2.jvt.modifiers;

import com.galtc2.jvt.Jvt;

import com.jerotes.jerotes.util.AttackFind;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 魔鳞升级(magicka_scale_upgrade)
 * 仿照"贵族强化"：前置部件含鳞熊鳞片，在匠魂站用魔鳞升级模板+钻石把护甲强化为魔鳞效果。
 * 效果按强化等级结算：
 * - 每级降低 23% 火焰/冰冻/闪电/溺水伤害(加算)；
 * - 4 级：减免 25% 魔法伤害(生命≤50% 时再减 15%)、免疫挤压/窒息/荆棘伤害；
 * - 4 级且生命≤50% 时获得速度。
 */
public class MagickaScaleUpgradeModifier extends Modifier implements ValidateModifierHook, ModifyDamageModifierHook, InventoryTickModifierHook {

    private static final MaterialId SCALY_BEAR = new MaterialId(Jvt.MODID, "scaly_bear_scale");
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };
    private static final float ELEMENTAL_REDUCTION = 0.23f;
    private static final float MAGIC_REDUCTION = 0.25f;
    private static final float LOW_HEALTH_MAGIC_REDUCTION = 0.15f;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.VALIDATE, ModifierHooks.MODIFY_HURT, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public Component validate(IToolStackView tool, ModifierEntry modifier) {
        for (MaterialVariant variant : tool.getMaterials().getList()) {
            if (variant.matches(SCALY_BEAR)) {
                return null;
            }
        }
        return Component.translatable("modifier.galtc2.magicka_scale_upgrade.requirements");
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0) {
            return amount;
        }
        EquipmentSlot firstSlot = null;
        int maxLevel = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken()) {
                int level = armor.getModifierLevel(this);
                if (level > 0) {
                    if (firstSlot == null) {
                        firstSlot = slot;
                    }
                    maxLevel = Math.max(maxLevel, level);
                }
            }
        }
        if (firstSlot == null || slotType != firstSlot) {
            return amount;
        }
        LivingEntity entity = context.getEntity();

        // 4 级免疫挤压/窒息/荆棘
        if (maxLevel >= 4 && (source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.THORNS))) {
            return 0;
        }
        // 每级 -23% 元素(火/冰/雷/溺水)
        if (AttackFind.isFireDamage(source) || AttackFind.isColdDamage(source) || AttackFind.isLightningDamage(source) || AttackFind.isWaterDamage(source)) {
            amount *= Math.max(0.0f, 1.0f - ELEMENTAL_REDUCTION * maxLevel);
        }
        // 4 级 -25%(半血再 -15%) 魔法
        if (maxLevel >= 4 && AttackFind.MagicResistance(source)) {
            float reduction = MAGIC_REDUCTION + (entity.getHealth() <= entity.getMaxHealth() / 2.0f ? LOW_HEALTH_MAGIC_REDUCTION : 0.0f);
            amount *= Math.max(0.0f, 1.0f - reduction);
        }
        return amount;
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide || !isCorrectSlot || !holder.isAlive()) {
            return;
        }
        // 4 级且生命 ≤50% 时获得速度
        if (modifier.getLevel() >= 4 && holder.getHealth() <= holder.getMaxHealth() / 2.0f) {
            holder.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 1, false, false), holder);
        }
    }
}
