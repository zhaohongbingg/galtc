package com.galtc2.modifiers;

import com.galtc2.Register.galtc2Effects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

public class oumunnbyou extends  BaseModifiers{

    /** 每支箭只掷一次骰子：穿射箭否则会每命中一只怪就再 roll 一遍（掷骰型必须每箭一次） */
    private static final ResourceLocation KEY_ARROW_ROLLED = ResourceLocation.fromNamespaceAndPath("galtc2", "oumunnbyou_arrow_rolled");
    /** 樱纹病持续时间（tick），与近战一致 */
    private static final int DURATION = 3000;
    /** amplifier 上限 */
    private static final int MAX_AMPLIFIER = 4;

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
        applySakura(livingTarget);
    }

    /**
     * 箭矢命中：沿用近战的「每级 20% 概率、樱纹病 {@value #DURATION} tick、amplifier 叠到 {@value #MAX_AMPLIFIER}」。
     * <p>
     * 概率掷骰是<b>每支箭一次</b>（{@value #KEY_ARROW_ROLLED}），穿射箭打一串怪时不会逐个重掷；
     * 掷中后按该箭命中的目标挂效果。
     */
    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level,
                                      Projectile projectile, AbstractArrow arrow, EntityHitResult hit,
                                      LivingEntity attacker, LivingEntity target) {
        if (arrow == null || attacker == null || target == null) return;
        if (target.level().isClientSide() || target.isDeadOrDying()) return;
        if (level <= 0) return;
        if (persistentData.contains(KEY_ARROW_ROLLED)) return;
        persistentData.putBoolean(KEY_ARROW_ROLLED, true);

        float chance = Math.min(level * 0.2F, 1.0F);
        if (target.getRandom().nextFloat() >= chance) return;
        applySakura(target);
    }

    /** 挂/叠樱纹病（近战与远程共用，参数与原来一致） */
    private static void applySakura(LivingEntity target) {
        MobEffectInstance old = target.getEffect(galtc2Effects.sakura.get());
        int newAmplifier = 0;
        if (old != null) {
            newAmplifier = old.getAmplifier() + 1;
        }
        newAmplifier = Math.min(newAmplifier, MAX_AMPLIFIER);
        target.addEffect(new MobEffectInstance(
                galtc2Effects.sakura.get(),
                DURATION,
                newAmplifier,
                false, true,
                true)
        );
    }
}
