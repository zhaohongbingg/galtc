package com.galtc2.modifiers;

import com.galtc2.GaltcConfig;
import com.galtc2.combat.DamageBreaker;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

public class narcissus extends BaseModifiers {

    /** 每级造成目标最大生命值的百分比 */
    private static final float EXTRA_DAMAGE_PER_LEVEL = 0.15F;

    /**
     * 远程额外伤害的封顶倍数：{@code extra ≤ 该箭 baseDamage × 3}。
     * <p>
     * 近战那套「目标最大生命 15%/级」照搬到弓上会失控：300 血的 BOSS 满级一箭就要追加上百点，
     * 而箭伤本身只有个位数。远程再加一道「不超过这一箭的 3 倍」的封顶，保留爆发感、
     * 又不让血量成长型目标把它放大成秒杀。近战分支不做这个封顶。
     */
    private static final float RANGED_CAP_MULTIPLE = 3.0F;

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

    /**
     * 箭矢命中：沿用近战的水仙口径——目标最大生命 {@value #EXTRA_DAMAGE_PER_LEVEL} × 等级，
     * 仍受 {@code GaltcConfig.NARCISSUS_MAX_EXTRA_DAMAGE} 上限约束、最低 1 点。
     * <p>
     * <b>不调 {@code target.hurt()}</b>：{@code PROJECTILE_HIT} 在箭矢自身结算之前派发，
     * 直接 hurt 会把 {@code invulnerableTime} 顶到 20，接下来箭矢自己的伤害就被吞掉。
     * 这里走匠魂的二次伤害入口 {@code attackEntitySecondary}，它会临时把无敌帧清零、结算后再还原。
     */
    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level,
                                      Projectile projectile, AbstractArrow arrow, EntityHitResult hit,
                                      LivingEntity attacker, LivingEntity target) {
        if (target == null || target.level().isClientSide() || target.isDeadOrDying()) {
            return;
        }
        if (level <= 0) {
            return;
        }

        float extraDamage = target.getMaxHealth() * EXTRA_DAMAGE_PER_LEVEL * level;
        // 远程专用封顶：不超过该箭 baseDamage 的 RANGED_CAP_MULTIPLE 倍（近战分支没有这一道）
        if (arrow != null) {
            extraDamage = Math.min(extraDamage, (float) arrow.getBaseDamage() * RANGED_CAP_MULTIPLE);
        }
        double cap = GaltcConfig.COMMON.NARCISSUS_MAX_EXTRA_DAMAGE.get();
        if (cap > 0.0D) {
            extraDamage = (float) Math.min(extraDamage, cap);
        }
        extraDamage = Math.max(1.0F, extraDamage);

        // 与近战一致：登记期望值供 DamageBreaker 兜底（目标有单次伤害硬上限时也能打满）
        if (GaltcConfig.COMMON.NARCISSUS_BREAK_ENABLED.get()) {
            DamageBreaker.expect(target, extraDamage);
        } else {
            DamageBreaker.discard(target);
        }

        DamageSource source = arrow != null && attacker != null
                ? attacker.damageSources().arrow(arrow, attacker)
                : target.damageSources().magic();
        if (!ToolAttackUtil.attackEntitySecondary(source, extraDamage, target, target, true)) {
            DamageBreaker.discard(target);
        }

        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_HURT_ON_FIRE, SoundSource.PLAYERS, 1.0F, 1.5F);
    }
}
