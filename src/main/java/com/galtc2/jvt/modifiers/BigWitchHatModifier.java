package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.spell.SpellList;
import net.minecraft.tags.DamageTypeTags;
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
 * 大巫婆之帽(big_witchs_hat)
 * 复刻 J村：受到魔法伤害时减伤 30%，并获得"魔法吸收"。
 * 占用升级槽。
 */
public class BigWitchHatModifier extends Modifier implements ModifyDamageModifierHook {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };
    private static final float MAGIC_REDUCTION = 0.3f;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MODIFY_HURT);
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0 || !source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            return amount;
        }
        // 只在第一件带此强化的护甲上结算一次
        EquipmentSlot firstSlot = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(this) > 0) {
                firstSlot = slot;
                break;
            }
        }
        if (firstSlot == null || slotType != firstSlot) {
            return amount;
        }
        LivingEntity entity = context.getEntity();
        if (!entity.level().isClientSide) {
            SpellList.MagicAbsorption(3, entity, entity).spellUse();
        }
        return amount * (1.0f - MAGIC_REDUCTION);
    }
}
