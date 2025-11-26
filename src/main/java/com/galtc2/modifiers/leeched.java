package com.galtc2.modifiers;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;

public class leeched  extends  BaseModifiers{
    public  leeched(){}
    @Override
    public boolean isNoLevels() {
        return true;
    }
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        // 去掉暴击和蓄力条件，只要造成伤害就触发
        if (context.isFullyCharged() && damageDealt > 0) {
            float percent = 0.5f * modifier.getEffectiveLevel();
            if (percent > 0) {
                LivingEntity attacker = context.getAttacker();
                attacker.heal(percent * damageDealt);
                attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), Sounds.NECROTIC_HEAL.getSound(), SoundSource.PLAYERS, 1.0f, 1.0f);

            }


        }}
        @Override
        public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target){
            if (target != null && attacker != null) {
                float percent = 0.03f * modifier.getEffectiveLevel(); // 降低基础值

                if (percent > 0) {
                    if (projectile instanceof AbstractArrow arrow) { // 去掉暴击要求
                        // 或者保留暴击有额外奖励
                        float finalPercent = percent;
                        if (arrow.isCritArrow()) {
                            finalPercent *= 1.5f; // 暴击多50%吸血
                        }

                        double estimatedDamage = arrow.getBaseDamage() * arrow.getDeltaMovement().length();
                        float healAmount = (float)(finalPercent * Math.min(target.getHealth(), estimatedDamage));

                        attacker.heal(healAmount);
                        // ...
                    }
                }
            }
            return false;
        }
    }

