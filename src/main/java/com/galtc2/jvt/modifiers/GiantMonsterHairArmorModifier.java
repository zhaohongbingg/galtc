package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.util.AttackFind;
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
 * 巨怪毛·护甲特性(御寒)
 * 复刻 Jerotes 巨怪套：受到冰冻类伤害时，每穿着一件含巨怪毛的护甲降低 16%(加算，4 件约 64%)。
 */
public class GiantMonsterHairArmorModifier extends Modifier implements ModifyDamageModifierHook {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };
    private static final float REDUCTION_PER_PIECE = 0.16f;
    private static final float MAX_REDUCTION = 0.8f;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MODIFY_HURT);
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0 || !AttackFind.isColdDamage(source)) {
            return amount;
        }
        // 统计穿着的件数(与 J村 一致：每件 -16%，加算)，只在第一件上结算一次
        int pieces = 0;
        EquipmentSlot firstSlot = null;
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
        float factor = 1.0f - Math.min(REDUCTION_PER_PIECE * pieces, MAX_REDUCTION);
        return amount * factor;
    }
}
