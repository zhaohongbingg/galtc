package com.galtc2.compat.meetyourfight.modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 亲人的羁绊 (kin_bond) —— 迎战 联动特性：命运之眷 材料的第三特性（攻防通用）。
 * <p>
 * 对应迎战 里用命运之眷 做的"甜心鸡尾酒之刃"（攻击时有概率给自己上药水效果）：
 * 近战命中时、以及被攻击时各有<b>基础 20% 概率</b>随机获得一个正面效果，持续时间见下方常量。
 * <p>
 * 概率受<b>自己的幸运值</b>影响，公式与系数照抄迎战 的 cocktailCutlassLuck：
 * 正幸运 {@code (1+0.5L)/((1/P)+0.5L)}，负幸运 {@code 1/((1/P)-L)}（等价于每点正幸运把相对几率 +50%）。
 * <p>
 * 护甲侧和 {@link HolyWarriorModifier} 一样，只由第一件带此特性的护甲结算，避免 4 件各自掷骰。
 */
public class KinBondModifier extends Modifier implements MeleeHitModifierHook, OnAttackedModifierHook {

    /** 基础触发概率（迎战 的 cocktailCutlassChance 默认值） */
    private static final double CHANCE = 0.2;
    /** buff 持续时间（8 秒） */
    private static final int DURATION = 8 * 20;
    /** 迎战 cocktailCutlassLuck 的系数：正幸运 分子0.5/分母0.5，负幸运 1 */
    private static final double LUCK_POSITIVE_NUMERATOR = 0.5;
    private static final double LUCK_POSITIVE_DENOMINATOR = 0.5;
    private static final double LUCK_NEGATIVE = 1.0;
    /** 可抽到的正面效果 */
    private static final MobEffect[] BUFFS = {
            MobEffects.MOVEMENT_SPEED,
            MobEffects.DAMAGE_BOOST,
            MobEffects.DAMAGE_RESISTANCE,
            MobEffects.REGENERATION
    };

    public KinBondModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.ON_ATTACKED);
    }

    /** 攻击时：有概率获得正面 buff */
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (context.getLevel().isClientSide()) {
            return;
        }
        LivingEntity attacker = context.getAttacker();
        if (roll(attacker)) {
            applyBuff(attacker);
        }
    }

    /** 被攻击时：有概率获得正面 buff（只在第一件护甲上结算） */
    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                           EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity entity = context.getEntity();
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (slotType != MeetYourFightHelper.firstArmorSlot(context, this)) {
            return;
        }
        if (roll(entity)) {
            applyBuff(entity);
        }
    }

    /** 按迎战 的幸运公式掷骰 */
    private static boolean roll(LivingEntity entity) {
        double chance = MeetYourFightHelper.luckChance(CHANCE, MeetYourFightHelper.luckOf(entity),
                LUCK_POSITIVE_NUMERATOR, LUCK_POSITIVE_DENOMINATOR, LUCK_NEGATIVE);
        return entity.getRandom().nextDouble() < chance;
    }

    private static void applyBuff(LivingEntity entity) {
        MobEffect effect = BUFFS[entity.getRandom().nextInt(BUFFS.length)];
        entity.addEffect(new MobEffectInstance(effect, DURATION, 0));
    }
}
