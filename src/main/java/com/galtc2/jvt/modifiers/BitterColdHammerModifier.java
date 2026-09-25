package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.util.AttackFind;
import com.jerotes.jerotes.util.Main;
import com.jerotes.jerotesvillage.init.JerotesVillageSoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * 苦寒之锤(bitter_cold_hammer)
 * 复刻 J村 苦寒之锤：
 * - 命中即冰冻目标到 280 tick，并破盾；
 * - 下落(>1.5格、非鞘翅)时近战命中触发"落地猛击"：按摔落距离追加伤害、范围击退、把攻击者弹起并重置摔落距离、音效/雪花粒子。
 * 占用能力槽，且不兼容其他武器能力强化。
 */
public class BitterColdHammerModifier extends Modifier implements MeleeDamageModifierHook, MeleeHitModifierHook, ValidateModifierHook {

    private static final float FALL_THRESHOLD = 1.5f;
    private static final int FREEZE_TICKS = 280;
    private static final double KNOCKBACK_RADIUS = 2.5;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.MELEE_HIT, ModifierHooks.VALIDATE);
    }

    @Override
    public Component validate(IToolStackView tool, ModifierEntry modifier) {
        return WeaponEnhancements.validateExclusive(tool, modifier);
    }

    /** 摔落追加伤害(与 J村 公式一致，暴击时 ×1.5) */
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity attacker = context.getAttacker();
        float distance = attacker.fallDistance;
        if (distance <= FALL_THRESHOLD || attacker.isFallFlying()) {
            return damage;
        }
        float extra = (float) (Math.min(6.0, 2.0 * distance) + Math.min(8.0, distance) + distance + 2.5 * distance);
        if (isFallCrit(attacker, context)) {
            extra *= 1.5f;
        }
        return damage + extra;
    }

    /** 命中：冰冻 + 破盾；满足下落条件则落地猛击(范围击退/弹起/特效) */
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target == null || !target.isAlive()) {
            return;
        }
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // 冰冻 + 破盾
        if (target.getTicksFrozen() < FREEZE_TICKS) {
            target.setTicksFrozen(FREEZE_TICKS);
        }
        if (target instanceof Player player) {
            player.disableShield(true);
        } else {
            target.stopUsingItem();
        }

        float distance = attacker.fallDistance;
        if (distance <= FALL_THRESHOLD || attacker.isFallFlying()) {
            return;
        }

        // 音效 + 雪花
        SoundEvent sound = distance > 5.0f ? JerotesVillageSoundEvents.MACE_SMASH_GROUND_HEAVY : JerotesVillageSoundEvents.MACE_SMASH_GROUND;
        serverLevel.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), sound, SoundSource.NEUTRAL, 1.0f, 1.0f);
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5), target.getZ(), 32, 0.3, 0.3, 0.3, 0.15);

        // 范围击退
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(KNOCKBACK_RADIUS));
        for (LivingEntity hit : nearby) {
            if (hit == attacker || !hit.isAlive()) {
                continue;
            }
            if (AttackFind.FindCanNotAttack(attacker, hit)) {
                continue;
            }
            Vec3 away = hit.position().subtract(target.position());
            double dist = away.length();
            if (dist < 0.01) {
                continue;
            }
            double power = (KNOCKBACK_RADIUS - Math.min(dist, KNOCKBACK_RADIUS)) * 0.6 * (1.0 - hit.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (!Main.mobSizeGiant(hit)) {
                power += 0.5;
            }
            if (power > 0) {
                Vec3 dir = away.normalize().scale(power);
                hit.push(dir.x, 0.6, dir.z);
            }
        }

        // 弹起攻击者并重置摔落距离
        attacker.setDeltaMovement(attacker.getDeltaMovement().add(0.0, 1.0, 0.0));
        attacker.resetFallDistance();
        attacker.setOnGround(false);
    }

    /** J村 暴击条件：下落中、未落地/未游泳/非失明/非载具/非疾跑 */
    private static boolean isFallCrit(LivingEntity attacker, ToolAttackContext context) {
        if (!(attacker instanceof Player player)) {
            return false;
        }
        return context.getLivingTarget() != null
                && player.fallDistance > 0.0f
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isPassenger()
                && !player.isSprinting();
    }
}
