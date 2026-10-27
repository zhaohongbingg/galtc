package com.galtc2.compat.meetyourfight.modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import com.galtc2.Galtc2;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;

/**
 * 斩月双刀 (moon_cleave) —— 迎战 联动特性：命运之眷 材料的攻击侧特性。
 * <p>
 * 对应迎战 里用命运之眷 做的"切割者之骰"：
 * <b>基础 20% 概率造成双倍伤害</b>，概率受<b>攻击方的幸运值</b>影响
 * （公式与系数照抄迎战：正幸运 {@code (1+L)/((1/P)+L)}，负幸运 {@code 1/((1/P)-3L)}，
 * 实现见 {@link MeetYourFightHelper#luckChance}）。
 * <p>
 * <b>近战</b>：掷骰放在 {@code MELEE_DAMAGE} 钩子里（和匠魂自带"导电"一样改 damage 数值），
 * 只改这次攻击的结算伤害，不会额外触发一次受击（不会撞无敌帧）。
 * <p>
 * <b>远程</b>（F-029 新增）：箭矢命中走 {@code PROJECTILE_HIT}，同样 20% 概率、同样双倍，额外伤害 =
 * 该箭 {@code baseDamage}（{@code MULTIPLIER - 1} 倍），用 {@link ToolAttackUtil#attackEntitySecondary}
 * 补结算 —— 绝不调 {@code target.hurt()}，否则会把 {@code invulnerableTime} 顶满、把箭矢自身的伤害吃掉。
 * <p>
 * <b>每支箭只掷一次骰</b>：标记写在箭矢自己的 persistentData（{@value #KEY_ARROW_ROLLED}）。
 * 穿射箭会对每个命中的目标各派发一次本 hook，不拦的话同一支箭能连着翻好几倍（F-029 §8-7）。
 */
public class MoonCleaveModifier extends Modifier implements MeleeDamageModifierHook, ProjectileHitModifierHook {

    /** 基础触发概率（迎战 的 Slicer's Dice 基础值，这里按你的设定取 0.2） */
    private static final double CHANCE = 0.2;
    /** 触发时的伤害倍率（2.0 = 双倍） */
    private static final float MULTIPLIER = 2.0f;
    /** 迎战 slicerDiceLuck 的系数：正幸运 分子1/分母1，负幸运 3 */
    private static final double LUCK_POSITIVE_NUMERATOR = 1.0;
    private static final double LUCK_POSITIVE_DENOMINATOR = 1.0;
    private static final double LUCK_NEGATIVE = 3.0;
    /** 箭矢去重键：一支箭只掷一次骰（穿射不滚雪球）。写在 projectile 的 TConstruct 持久数据里 */
    private static final ResourceLocation KEY_ARROW_ROLLED = Galtc2.loc("moon_cleave_rolled");

    public MoonCleaveModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                float baseDamage, float damage) {
        // 只在服务端掷骰，客户端预测走原伤害
        if (context.getAttacker().level().isClientSide()) {
            return damage;
        }
        LivingEntity target = context.getLivingTarget();
        if (target == null || damage <= 0) {
            return damage;
        }
        LivingEntity attacker = context.getAttacker();
        if (roll(attacker)) {
            return damage * MULTIPLIER;
        }
        return damage;
    }

    /**
     * 远程：箭矢命中时同样掷一次骰，命中则补一发额外伤害。
     * <p>额外伤害取 {@code (MULTIPLIER - 1) × 箭矢 baseDamage}，总和就是"双倍"。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null || !target.isAlive()) {
            return false;
        }
        if (projectile.level().isClientSide() || !(projectile instanceof AbstractArrow arrow)) {
            return false;
        }
        // 每支箭只掷一次：先打标记再掷，掷不中也算用掉（穿射的第二个目标不再判）
        if (persistentData.getBoolean(KEY_ARROW_ROLLED)) {
            return false;
        }
        persistentData.putBoolean(KEY_ARROW_ROLLED, true);

        float baseDamage = (float) arrow.getBaseDamage();
        if (baseDamage <= 0.0f) {
            return false;
        }
        if (!roll(attacker)) {
            return false;
        }
        // 额外伤害归属到箭矢（attacker 是玩家时 playerAttack 口径见 Valuation 的同类处理，这里用 arrow 口径即可）
        DamageSource source = attacker.damageSources().arrow(arrow, attacker);
        ToolAttackUtil.attackEntitySecondary(source, baseDamage * (MULTIPLIER - 1.0f), target, target, true);
        return false;
    }

    /** 按幸运值修正后的概率掷一次骰（近战/远程共用） */
    private static boolean roll(LivingEntity attacker) {
        double chance = MeetYourFightHelper.luckChance(CHANCE, MeetYourFightHelper.luckOf(attacker),
                LUCK_POSITIVE_NUMERATOR, LUCK_POSITIVE_DENOMINATOR, LUCK_NEGATIVE);
        return attacker.getRandom().nextDouble() < chance;
    }
}
