package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.util.Main;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 明路巨兽角(bright_land_beast_horn)
 * 复刻 J村：近战伤害随目标体型增大(宽×高)。
 * 无槽位、不互斥。
 */
public class BrightLandBeastHornModifier extends Modifier implements MeleeDamageModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity target = context.getLivingTarget();
        if (target == null || baseDamage <= 0) {
            return damage;
        }
        // 体型加成：宽 × 4 × 高，封顶 baseDamage*10
        float scaled = Main.mobWidth(target) * 4.0f * Main.mobHeight(target);
        float cap = baseDamage * 10.0f;
        if (scaled > cap) {
            scaled = cap;
        }
        float result;
        if (damage / 2.0f * scaled < 2.0f) {
            result = damage + 2.0f;
        } else {
            result = damage + damage / baseDamage * scaled;
        }
        if (Float.isNaN(result) || Float.isInfinite(result)) {
            result = 0.0f;
        }
        return result;
    }
}
