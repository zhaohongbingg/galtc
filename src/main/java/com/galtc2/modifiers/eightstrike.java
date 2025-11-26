package com.galtc2.modifiers;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class eightstrike extends BaseModifiers{
    public  eightstrike(){}
    @Override
    public boolean isNoLevels() {
        return false;
    }

    public class EightStrike extends BaseModifiers {



        @Override
        public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier,
                                  ToolAttackContext context, float damageDealt) {
            LivingEntity target = context.getLivingTarget();
            Player attacker = context.getPlayerAttacker();

            if (target != null && attacker != null && damageDealt > 0) {
                int strikes = modifier.getLevel(); // 根据等级决定连击次数

                for (int i = 1; i < strikes; i++) { // 从1开始，因为第一击已经造成了
                    // 重置无敌帧
                    target.invulnerableTime = 0;
                    target.hurtTime = 0;

                    // 造成额外的伤害
                    float extraDamage = damageDealt * 0.8f; // 后续攻击伤害减半

                    target.hurt(attacker.damageSources().playerAttack(attacker), extraDamage);

                    // 添加粒子效果
                    if (!target.level().isClientSide) {
                        addStrikeEffect(target, i);
                    }
                }
            }
        }

        private void addStrikeEffect(LivingEntity target, int strikeNum) {
            // 添加连击的视觉效果
            target.level().addParticle(ParticleTypes.SWEEP_ATTACK,
                    target.getX() + (Math.random() - 0.5) * 2,
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ() + (Math.random() - 0.5) * 2,
                    0, 0.1, 0);
        }
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        int level = modifier.getLevel();

        // 每级乘以1.5倍攻速
        // 1级 = 1.5倍，2级 = 2.25倍，3级 = 3.375倍...
        float multiplier = (float) Math.pow(5, level);
        ToolStats.ATTACK_SPEED.multiply(builder, multiplier);
    }

}

