package com.galtc2.modifiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CamusModifier extends Modifier implements MeleeHitModifierHook, OnAttackedModifierHook, ProjectileHitModifierHook {

    private static final Map<UUID, Long> CURSED_ENTITIES = new ConcurrentHashMap<>();
    /**
     * 禁疗持续时长（tick）。正式值：200 tick = 10 秒（此前测试期曾临时放长到 1200 tick，已还原）。
     * camus / camus_melee / camus_armor 三个词条共用此常量，不要各写一份。
     */
    private static final long CURSE_DURATION = 200; // 10秒（正式值）

    /** 全局治疗监听只挂一次（拆出 _melee / _armor 后会创建多个实例，避免重复注册） */
    private static boolean listenerRegistered = false;

    public CamusModifier() {
        if (!listenerRegistered) {
            listenerRegistered = true;
            MinecraftForge.EVENT_BUS.register(this);
        }
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

    /**
     * 箭矢命中：晴空下的加缪——天气良好时给被射中的目标挂禁疗（与近战同一时长 {@link #CURSE_DURATION}）。
     * <p>
     * 实现在父类里，但 {@code PROJECTILE_HIT} 钩子只由 {@link CamusMeleeModifier} 注册，
     * 所以 {@link CamusArmorModifier} 不会因为继承而多出远程分支。
     * 用箭矢存下来的 {@code modifier.getLevel()} 只是标记"这条词条在弓上"，禁疗本身不随等级变化。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null) return false;
        if (attacker.level().isClientSide()) return false;
        if (!(projectile instanceof AbstractArrow)) return false;
        if (isGoodWeather(attacker)) {
            applyCurse(target);
        }
        return false;
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
