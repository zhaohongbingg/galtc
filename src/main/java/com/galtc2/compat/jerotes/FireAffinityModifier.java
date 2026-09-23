package com.galtc2.compat.jerotes;

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
 * 火焰亲和特性基类 —— J村火系联动。
 * <p>
 * 护甲侧：每件降低一定比例的火系承伤（复刻 J村 火焰使徒/蛇龙的判定）。
 * 输出侧：{@link JerotesEvents} 统计穿戴件数，提升造成的火系伤害。
 */
public abstract class FireAffinityModifier extends Modifier implements ModifyDamageModifierHook {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    /** 每件降低的火系承伤比例 */
    public abstract float fireTakenReduction();

    /** 每件提升的火系输出比例 */
    public abstract float fireDealtBonus();

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MODIFY_HURT);
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                                   EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0 || !AttackFind.isFireDamage(source)) {
            return amount;
        }
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
        return amount * Math.max(0.0f, 1.0f - fireTakenReduction() * pieces);
    }
}
