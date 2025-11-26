package com.galtc2.modifiers;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class narcissus extends BaseModifiers{
    public  narcissus(){

    }
    @Override
    public boolean isNoLevels() {
        return false;
    }
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        Entity entity = context.getTarget();
        if (entity instanceof LivingEntity target) {
            if (target != null) {
                // 每级造成目标最大血量15%的额外伤害
                float percentDamage = 0.15f * modifier.getEffectiveLevel();
                float extraDamage = target.getMaxHealth() * percentDamage;

                // 设置最小和最大伤害限制
                extraDamage = Math.max(1.0f, Math.min(extraDamage, 1000.0f)); // 最少1点，最多20点

                // 造成真实伤害（无视护甲）
                target.hurt(target.damageSources().magic(), extraDamage);

                // 特效
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_HURT_ON_FIRE, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
        }
    }
}
