package modifiers;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;

public class Valuation extends BaseModifiers{
    public  Valuation(){}
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (damageDealt > 0) {
            Entity entity = context.getTarget();
            LivingEntity attacker = context.getAttacker();

            if (entity instanceof LivingEntity target && attacker != null) {
                // 获取幸运值
                float luck = attacker instanceof Player player ?
                        (float)player.getAttributeValue(Attributes.LUCK) : 0.0f;

                // 幸运值影响多个方面
                float adjustedLuck = Math.max(-10.0f, Math.min(10.0f, luck)); // 限制幸运值范围

                // 1. 影响自伤概率（幸运越高自伤概率越低）
                float selfDamageChance = 0.1f - (adjustedLuck * 0.008f); // 每点幸运降低0.8%
                selfDamageChance = Math.max(0.01f, Math.min(0.2f, selfDamageChance)); // 限制在1%-20%

                if (RANDOM.nextFloat() < selfDamageChance) {
                    // 自伤：幸运值影响自伤程度
                    float selfDamageMultiplier = 1.0f - (adjustedLuck * 0.05f); // 幸运值降低自伤
                    selfDamageMultiplier = Math.max(0.1f, selfDamageMultiplier);

                    float selfDamage = damageDealt * selfDamageMultiplier;
                    attacker.hurt(attacker.damageSources().magic(), selfDamage);

                    // 负面特效
                    attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                            SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 0.8f);

                } else {
                    // 额外伤害：幸运值影响伤害倍率
                    float minMultiplier = 1.0f + Math.max(0, adjustedLuck * 0.5f); // 幸运提高最低倍率
                    float maxMultiplier = 99.0f + Math.max(0, adjustedLuck * 2.0f); // 幸运提高最高倍率

                    float multiplier = minMultiplier + RANDOM.nextFloat() * (maxMultiplier - minMultiplier);
                    multiplier = Math.min(multiplier, 99.0f);

                    float extraDamage = damageDealt * multiplier;
                    target.hurt(attacker.damageSources().playerAttack(attacker instanceof Player ? (Player)attacker : null), extraDamage);

                    // 根据倍率调整特效强度
                    float pitch = 0.5f + (multiplier / 99.0f) * 1.0f;
                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 2.0f, pitch);

                    // 粒子效果
                    if (target.level() instanceof ServerLevel serverLevel) {
                        int particleCount = (int)Math.max(5, multiplier / 5);
                        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                                target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(),
                                particleCount, 0.3, 0.3, 0.3, 0.2);
                    }

                    // 显示倍率信息给玩家
                    if (attacker instanceof Player player && multiplier > 10.0f) {
                        player.displayClientMessage(Component.literal(String.format("§6幸运一击！ %.1fx 伤害!", multiplier)), true);
                    }
                }
            }
        }
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (target != null && attacker != null && projectile instanceof AbstractArrow arrow) {

            // 获取幸运值
            float luck = attacker instanceof Player player ?
                    (float)player.getAttributeValue(Attributes.LUCK) : 0.0f;

            // 估算基础伤害
            float baseDamage = (float)(arrow.getBaseDamage() * arrow.getDeltaMovement().length() / 3.0f);

            float adjustedLuck = Math.max(-10.0f, Math.min(10.0f, luck));
            float selfDamageChance = 0.1f - (adjustedLuck * 0.008f);
            selfDamageChance = Math.max(0.01f, Math.min(0.2f, selfDamageChance));

            if (RANDOM.nextFloat() < selfDamageChance) {
                // 弓箭自伤（较轻）
                float selfDamage = baseDamage * 0.3f * (1.0f - adjustedLuck * 0.05f);
                attacker.hurt(attacker.damageSources().magic(), Math.max(1.0f, selfDamage));

            } else {
                // 弓箭额外伤害
                float minMultiplier = 1.0f + Math.max(0, adjustedLuck * 0.3f);
                float maxMultiplier = 50.0f + Math.max(0, adjustedLuck * 1.5f); // 弓箭倍率相对较低

                float multiplier = minMultiplier + RANDOM.nextFloat() * (maxMultiplier - minMultiplier);
                float extraDamage = baseDamage * multiplier;

                target.hurt(attacker.damageSources().playerAttack(attacker instanceof Player ? (Player)attacker : null), extraDamage);

                // 视觉效果
                if (target.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(),
                            (int)(multiplier / 3), 0.3, 0.3, 0.3, 0.2);
                }
            }
        }
        return false;
    }






}
