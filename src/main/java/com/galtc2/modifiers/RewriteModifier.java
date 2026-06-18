package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RewriteModifier extends Modifier implements MeleeHitModifierHook {

    private static final UUID ARMOR_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
    private static final UUID HEALTH_UUID = UUID.fromString("d4e5f6a7-b8c9-0123-defa-234567890123");
    private static final long DURATION_TICKS = 1200; // 60秒

    private static final Map<UUID, Map<UUID, Long>> ATTACKER_TARGET_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<UUID, Double>> ATTACKER_TARGET_ARMOR = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<UUID, Double>> ATTACKER_TARGET_HEALTH = new ConcurrentHashMap<>();

    public RewriteModifier() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        if (attacker == null || attacker.level().isClientSide()) return;
        LivingEntity target = context.getLivingTarget();
        if (target == null) return;

        UUID attackerId = attacker.getUUID();
        UUID targetId = target.getUUID();

        double targetArmor = target.getArmorValue();
        double targetMaxHealth = target.getMaxHealth();

        ATTACKER_TARGET_TIMESTAMPS.computeIfAbsent(attackerId, k -> new ConcurrentHashMap<>()).put(targetId, attacker.level().getGameTime());
        ATTACKER_TARGET_ARMOR.computeIfAbsent(attackerId, k -> new ConcurrentHashMap<>()).put(targetId, targetArmor);
        ATTACKER_TARGET_HEALTH.computeIfAbsent(attackerId, k -> new ConcurrentHashMap<>()).put(targetId, targetMaxHealth);

        updateBonus(attacker);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        UUID targetId = target.getUUID();

        for (Map<UUID, Long> timestamps : ATTACKER_TARGET_TIMESTAMPS.values()) {
            timestamps.remove(targetId);
        }
        for (Map<UUID, Double> armorMap : ATTACKER_TARGET_ARMOR.values()) {
            armorMap.remove(targetId);
        }
        for (Map<UUID, Double> healthMap : ATTACKER_TARGET_HEALTH.values()) {
            healthMap.remove(targetId);
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        UUID entityId = entity.getUUID();
        if (!ATTACKER_TARGET_TIMESTAMPS.containsKey(entityId)) return;

        long currentTime = entity.level().getGameTime();
        Map<UUID, Long> timestamps = ATTACKER_TARGET_TIMESTAMPS.get(entityId);
        if (timestamps == null) return;

        timestamps.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > DURATION_TICKS) {
                UUID targetId = entry.getKey();
                Map<UUID, Double> armorMap = ATTACKER_TARGET_ARMOR.get(entityId);
                Map<UUID, Double> healthMap = ATTACKER_TARGET_HEALTH.get(entityId);
                if (armorMap != null) armorMap.remove(targetId);
                if (healthMap != null) healthMap.remove(targetId);
                return true;
            }
            return false;
        });

        updateBonus(entity);
    }

    private void updateBonus(LivingEntity attacker) {
        UUID attackerId = attacker.getUUID();
        Map<UUID, Double> armorMap = ATTACKER_TARGET_ARMOR.get(attackerId);
        Map<UUID, Double> healthMap = ATTACKER_TARGET_HEALTH.get(attackerId);

        double totalArmor = 0;
        double totalHealth = 0;

        if (armorMap != null) {
            for (Double armor : armorMap.values()) {
                totalArmor += armor;
            }
        }
        if (healthMap != null) {
            for (Double health : healthMap.values()) {
                totalHealth += health;
            }
        }

        AttributeInstance armorAttr = attacker.getAttribute(Attributes.ARMOR);
        AttributeInstance healthAttr = attacker.getAttribute(Attributes.MAX_HEALTH);

        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_UUID);
            if (totalArmor > 0) {
                armorAttr.addPermanentModifier(new AttributeModifier(ARMOR_UUID, "rewrite_armor", totalArmor, AttributeModifier.Operation.ADDITION));
            }
        }

        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_UUID);
            if (totalHealth > 0) {
                healthAttr.addPermanentModifier(new AttributeModifier(HEALTH_UUID, "rewrite_health", totalHealth, AttributeModifier.Operation.ADDITION));
            }
        }
    }
}