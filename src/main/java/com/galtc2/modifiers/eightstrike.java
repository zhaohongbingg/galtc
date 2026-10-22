package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/**
 * 八段刺（零壹材料特性）：
 * <ul>
 *   <li>攻击速度 ×8</li>
 *   <li>攻击力 -80%（代价，和攻速绑定在同一个特性上）</li>
 *   <li>命中后清零目标的受击无敌帧，让每一刀都能结算</li>
 * </ul>
 * 代价必须放在特性里而不是材料属性里：TC 的材料特性「只要有一个该材料部件就生效」，
 * 而材料属性只对用到该部位的工具生效。写进手柄属性的话，零壹只做刀头就能白嫖 8 倍攻速。
 */
public class eightstrike extends BaseModifiers {

    /** 攻速倍率，固定 8 倍（不吃等级） */
    private static final float ATTACK_SPEED_MULTIPLIER = 8.0F;

    /** 攻击力惩罚：-80% */
    private static final float ATTACK_DAMAGE_PENALTY = -0.8F;

    public eightstrike() {}

    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        ToolStats.ATTACK_SPEED.multiply(builder, ATTACK_SPEED_MULTIPLIER);
        ToolStats.ATTACK_DAMAGE.percent(builder, ATTACK_DAMAGE_PENALTY);
    }

    /**
     * 无视无敌帧。
     *
     * <p>主手这一刀会把目标的 {@code invulnerableTime} 顶到 20，攻速 ×8 之后
     * 后续的攻击基本全部落在无敌帧里，会在 {@code hurt()} 的
     * {@code if (invulnerableTime > 10 && amount <= lastHurt) return false;} 被直接丢弃。
     * 所以命中后立刻清零，让下一刀能正常结算。
     */
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        if (target == null || target.level().isClientSide()) {
            return;
        }
        target.invulnerableTime = 0;
        target.hurtTime = 0;
    }
}
