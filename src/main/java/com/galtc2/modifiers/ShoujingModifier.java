package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class ShoujingModifier extends Modifier implements MeleeHitModifierHook, OnAttackedModifierHook {
    public interface Summoner {
        void summon(Level level, LivingEntity owner, LivingEntity target);
    }

    private static volatile Summoner summoner;
    private static final long COOLDOWN = 40L;
    private static final String KEY_ATTACK = "galtc2_shoujing_attack";
    private static final String KEY_DEFENSE = "galtc2_shoujing_defense";

    public static void setSummoner(Summoner impl) { summoner = impl; }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity owner = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (summoner == null || owner == null || target == null || owner.level().isClientSide() || damageDealt <= 0) return;
        if (ready(owner, KEY_ATTACK)) summoner.summon(owner.level(), owner, target);
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType,
                           DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity owner = context.getEntity();
        if (summoner == null || owner.level().isClientSide() || amount <= 0) return;
        if (source.getEntity() instanceof LivingEntity attacker && ready(owner, KEY_DEFENSE)) {
            summoner.summon(owner.level(), owner, attacker);
        }
    }

    private static boolean ready(LivingEntity entity, String key) {
        long now = entity.level().getGameTime();
        long last = entity.getPersistentData().getLong(key);
        if (now - last < COOLDOWN) return false;
        entity.getPersistentData().putLong(key, now);
        return true;
    }
}
