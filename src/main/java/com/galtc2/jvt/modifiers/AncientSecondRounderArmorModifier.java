package com.galtc2.jvt.modifiers;

import com.jerotes.jerotesvillage.util.OtherEntityFactionFind;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 古二轮金属（护甲侧）：来自复魂尸 / 沉降类生物的伤害减免 25%。
 * <p>
 * 判定沿用工具侧 {@link AncientSecondRounderModifier} 的 {@code OtherEntityFactionFind.isResurrecter}，
 * 保证「增伤」和「减伤」两条特性认的是同一批生物。
 */
public class AncientSecondRounderArmorModifier extends Modifier implements ModifyDamageModifierHook {

    private static final float REDUCTION = 0.25F;

    public AncientSecondRounderArmorModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MODIFY_HURT);
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0.0F) {
            return amount;
        }
        Entity attacker = source.getEntity();
        if (attacker != null && OtherEntityFactionFind.isResurrecter(attacker.getType())) {
            return amount * (1.0F - REDUCTION);
        }
        return amount;
    }
}
