package com.galtc2.jvt.modifiers;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 苦寒之锤(bitter_cold_hammer)
 * 只保留"按下落高度追加伤害"这一条：下落(>1.5格、非鞘翅)时近战命中额外加伤，暴击时 ×1.5。
 * <p>
 * 冰冻、破盾、范围击退、弹起、音效/粒子等效果已移除；
 * 互斥校验也一并去掉，可以与其他能力强化共存。
 */
public class BitterColdHammerModifier extends Modifier implements MeleeDamageModifierHook {

    private static final float FALL_THRESHOLD = 1.5f;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }

    /** 摔落追加伤害(与 J村 公式一致，暴击时 ×1.5) */
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity attacker = context.getAttacker();
        if (attacker == null) {
            return damage;
        }
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
