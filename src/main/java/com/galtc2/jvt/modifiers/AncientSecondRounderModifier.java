package com.galtc2.jvt.modifiers;

import com.jerotes.jerotesvillage.util.OtherEntityFactionFind;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 古二轮金属(ancient_second_rounder) 材料特性：复魂尸杀手。
 * 仿 J村 bane_of_humanoids：对沉降/复魂尸类生物( tag jerotes:resurrecter )追加伤害，
 * 公式 = damage + damage/baseDamage * 2.5 * 等级。
 */
public class AncientSecondRounderModifier extends Modifier implements MeleeDamageModifierHook {

    private static final float BONUS_PER_LEVEL = 2.5f;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity target = context.getLivingTarget();
        if (target == null || baseDamage <= 0 || !OtherEntityFactionFind.isResurrecter(target.getType())) {
            return damage;
        }
        float result = damage + damage / baseDamage * BONUS_PER_LEVEL * modifier.getLevel();
        if (Float.isNaN(result) || Float.isInfinite(result)) {
            return 0.0f;
        }
        return result;
    }
}
