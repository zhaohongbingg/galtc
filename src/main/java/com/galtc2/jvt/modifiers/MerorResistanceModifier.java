package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.util.EntityAndItemFind;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 莫厄减伤 (meror_resistance)
 * 复刻 Jerotes 莫厄制式套的减伤效果：
 *  - 非莫厄来源伤害每级减伤 12%(莫厄伤害不减伤)；
 *  - 穿戴多件带此特性的护甲时等级叠加(1-4 满级)，4 件 1 级等效 4 级(48%)。
 */
public class MerorResistanceModifier extends Modifier implements ModifyDamageModifierHook {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private static final float REDUCTION_PER_LEVEL = 0.12f;
    private static final int MAX_LEVEL = 4;

    public MerorResistanceModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MODIFY_HURT);
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0) return amount;
        if (EntityAndItemFind.isMerorAttack(source)) return amount;

        int totalLevel = 0;
        EquipmentSlot firstSlot = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken()) {
                int level = armor.getModifierLevel(this);
                if (level > 0) {
                    if (firstSlot == null) firstSlot = slot;
                    totalLevel += level;
                }
            }
        }
        totalLevel = Math.min(totalLevel, MAX_LEVEL);
        if (firstSlot == null || slotType != firstSlot) {
            return amount;
        }
        return amount * (1.0f - REDUCTION_PER_LEVEL * totalLevel);
    }
}
