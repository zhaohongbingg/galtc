package com.galtc2.modifiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CamusModifier extends Modifier implements MeleeHitModifierHook, OnAttackedModifierHook {

    private static final Map<UUID, Long> CURSED_ENTITIES = new ConcurrentHashMap<>();
    private static final long CURSE_DURATION = 200; // 10秒

    public CamusModifier() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.ON_ATTACKED);
    }

    private boolean isGoodWeather(LivingEntity entity) {
        return !entity.level().isRaining() && !entity.level().isNight();
    }

    private void applyCurse(LivingEntity target) {
        CURSED_ENTITIES.put(target.getUUID(), target.level().getGameTime() + CURSE_DURATION);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (context.getAttacker() == null || context.getAttacker().level().isClientSide()) return;
        LivingEntity target = context.getLivingTarget();
        if (target == null) return;

        if (isGoodWeather(context.getAttacker())) {
            applyCurse(target);
        }
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, net.minecraft.world.damagesource.DamageSource source, float amount, boolean isDirectDamage) {
        if (context.getEntity().level().isClientSide()) return;
        Entity attackerEntity = source.getEntity();
        if (attackerEntity instanceof LivingEntity attacker) {
            if (isGoodWeather(context.getEntity())) {
                applyCurse(attacker);
            }
        }
    }

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        Long expireTime = CURSED_ENTITIES.get(entity.getUUID());
        if (expireTime != null) {
            if (entity.level().getGameTime() < expireTime) {
                event.setCanceled(true);
            } else {
                CURSED_ENTITIES.remove(entity.getUUID());
            }
        }
    }
}
