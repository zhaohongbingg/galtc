package com.galtc2.compat.meetyourfight.modifiers;

import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 斩月双刀 (moon_cleave) —— 迎战 联动特性：命运之眷 材料的攻击侧特性。
 * <p>
 * 对应迎战 里用命运之眷 做的"切割者之骰"：
 * 近战命中前 <b>基础 20% 概率造成双倍伤害</b>，概率受<b>攻击方的幸运值</b>影响
 * （公式与系数照抄迎战：正幸运 {@code (1+L)/((1/P)+L)}，负幸运 {@code 1/((1/P)-3L)}）。
 * <p>
 * 概率掷骰放在 {@code MELEE_DAMAGE} 钩子里（和匠魂自带"导电"一样改 damage 数值），
 * 只改这次攻击的结算伤害，不会额外触发一次受击（不会撞无敌帧）。
 */
public class MoonCleaveModifier extends Modifier implements MeleeDamageModifierHook {

    /** 基础触发概率（迎战 的 Slicer's Dice 基础值，这里按你的设定取 0.2） */
    private static final double CHANCE = 0.2;
    /** 触发时的伤害倍率（2.0 = 双倍） */
    private static final float MULTIPLIER = 2.0f;
    /** 迎战 slicerDiceLuck 的系数：正幸运 分子1/分母1，负幸运 3 */
    private static final double LUCK_POSITIVE_NUMERATOR = 1.0;
    private static final double LUCK_POSITIVE_DENOMINATOR = 1.0;
    private static final double LUCK_NEGATIVE = 3.0;

    public MoonCleaveModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
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
        double chance = MeetYourFightHelper.luckChance(CHANCE, MeetYourFightHelper.luckOf(attacker),
                LUCK_POSITIVE_NUMERATOR, LUCK_POSITIVE_DENOMINATOR, LUCK_NEGATIVE);
        if (attacker.getRandom().nextDouble() < chance) {
            return damage * MULTIPLIER;
        }
        return damage;
    }
}
