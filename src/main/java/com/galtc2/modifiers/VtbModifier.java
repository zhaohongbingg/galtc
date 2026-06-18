package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class VtbModifier extends Modifier implements MeleeDamageModifierHook {

    public VtbModifier() {}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if (context.getAttacker() == null || context.getAttacker().level().isClientSide()) {
            return damage;
        }
        
        LivingEntity attacker = context.getAttacker();
        
        // 获取玩家的最大生命值
        float maxHealth = attacker.getMaxHealth();
        
        // 附加额外伤害（最大生命值的50%）
        float extraDamage = maxHealth * 0.5f;
        
        return damage + extraDamage;
    }
}
