package com.galtc2.modifiers;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtc2Effects;
import com.galtc2.content.effect.sakura;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class oumunnbyou extends  BaseModifiers{
    public oumunnbyou() {};
    @Override
    public boolean isNoLevels() {
        return false;
    }


    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        super.afterMeleeHit(tool, modifier, context, damageDealt);
        Entity target = context.getTarget();
        LivingEntity attacker = context.getAttacker();
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        int modifierLevel = modifier.getLevel();
        float chance = (float) Math.min(modifierLevel*0.2,1f);
        if(livingTarget.getRandom().nextFloat()>=chance){
            return;
        }
        MobEffectInstance old = livingTarget.getEffect(galtc2Effects.sakura.get());

        int newAmplifier = 0;
        if(old!=null){
            newAmplifier=old.getAmplifier()+1;

        }
        newAmplifier =Math.min(newAmplifier, 4);

        livingTarget.addEffect(new MobEffectInstance(
                galtc2Effects.sakura.get(),
                3000,
                newAmplifier,
                false, true,
                true)

        );







    }}


