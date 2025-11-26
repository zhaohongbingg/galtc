package com.galtc2.content.effect;

import com.galtc2.Register.galtc2Effects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class sakura extends MobEffect {

    public sakura() {
        super(MobEffectCategory.HARMFUL, 0xFFC0CB);
    }
    private int maxLayers=5;
    public  void applyEffectTick(LivingEntity entity,int amplifier) {
        int currentLayers = amplifier + 1;
        if (currentLayers >= maxLayers) {
            if (!entity.level().isClientSide()) {
                entity.hurt(entity.damageSources().magic(), Float.MAX_VALUE);
                entity.setHealth(0);
                }
            }}

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
    }

