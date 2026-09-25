package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.util.AttackFind;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 巨怪毛·近战特性(霜噬)
 * 近战命中后，附加与本次伤害等量的冰冻类伤害(使用 J村 冰冻免疫所识别的 jerotes:freeze_melee)。
 */
public class GiantMonsterHairMeleeModifier extends Modifier implements MeleeHitModifierHook {

    /** J村 冰冻类伤害(在 minecraft:is_freezing tag 内)，会被巨怪套的减伤识别 */
    private static final ResourceKey<DamageType> FREEZE_MELEE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jerotes", "freeze_melee"));

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (damageDealt <= 0) {
            return;
        }
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        Level level = context.getLevel();
        if (target == null || !target.isAlive() || level.isClientSide) {
            return;
        }
        DamageSource frost = AttackFind.findDamageType(attacker, FREEZE_MELEE, attacker);
        target.hurt(frost, damageDealt);
    }
}
