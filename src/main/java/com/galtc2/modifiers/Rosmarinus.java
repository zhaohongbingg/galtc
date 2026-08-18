package com.galtc2.modifiers;


import com.galtc2.Register.galtc2Effects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.function.BiConsumer;

public class Rosmarinus extends BaseModifiers {

    //迷迭香
    public Rosmarinus() {
    }
    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        if (target != null) {
            target.addEffect(new MobEffectInstance(galtc2Effects.STUN.get(), 40, 0, false, false, true));
        }
    }

    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level, Projectile projectile, AbstractArrow arrow, EntityHitResult hit, LivingEntity attacker, LivingEntity target) {
        if (target != null) {
            target.addEffect(new MobEffectInstance(galtc2Effects.STUN.get(), 40, 0, false, false, true));
        }
    }
}
