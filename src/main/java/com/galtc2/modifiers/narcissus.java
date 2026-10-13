package com.galtc2.modifiers;

import com.galtc2.GaltcConfig;
import com.galtc2.combat.DamageBreaker;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class narcissus extends BaseModifiers {

    /** 每级造成目标最大生命值的百分比 */
    private static final float EXTRA_DAMAGE_PER_LEVEL = 0.15F;

    public narcissus() {
    }

    @Override
    public boolean isNoLevels() {
        return false;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        if (target == null || target.level().isClientSide()) {
            return;
        }

        float level = modifier.getEffectiveLevel();
        if (level <= 0.0F) {
            return;
        }

        // 每级造成目标最大血量 15% 的额外伤害
        float extraDamage = target.getMaxHealth() * EXTRA_DAMAGE_PER_LEVEL * level;
        double cap = GaltcConfig.COMMON.NARCISSUS_MAX_EXTRA_DAMAGE.get();
        if (cap > 0.0D) {
            extraDamage = (float) Math.min(extraDamage, cap);
        }
        extraDamage = Math.max(1.0F, extraDamage);

        DamageSource source = target.damageSources().magic();

        if (GaltcConfig.COMMON.NARCISSUS_BREAK_ENABLED.get()) {
            // 这次额外伤害是本模组自己发起的，我们确切知道它「本该」打多少 ——
            // 不需要猜被削掉了多少，直接登记期望值，交给 DamageBreakerEvents 兜底。
            // 这样无论目标是原版生物、还是像 GalBoss 那样在 hurt() 覆写里做了单次伤害硬上限，
            // 都能打出完整数值。
            DamageBreaker.expect(target, extraDamage);
        } else {
            DamageBreaker.discard(target);
        }

        if (GaltcConfig.COMMON.NARCISSUS_BREAK_RESET_COOLDOWN.get()) {
            // 主手那一刀刚刚把目标的受击无敌时间顶满（invulnerableTime = 20）。
            // 不复位的话，这次额外伤害在 hurt() 里会走
            // `if (invulnerableTime > 10) { if (pAmount <= lastHurt) return false; }`
            // 直接被丢弃，连伤害事件都不会触发 —— 破限也就无从谈起。
            // 只复位水仙自己这一下需要的状态，不影响其他来源的受击节奏。
            target.invulnerableTime = 0;
            target.hurtTime = 0;
        }

        if (!target.hurt(source, extraDamage)) {
            // 攻击没被接受（免疫、阶段无敌等），把登记项清掉，免得留给本刻后续的伤害。
            DamageBreaker.discard(target);
        }

        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_HURT_ON_FIRE, SoundSource.PLAYERS, 1.0F, 1.5F);
    }
}
