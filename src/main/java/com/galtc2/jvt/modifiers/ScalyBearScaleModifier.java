package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.util.AttackFind;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 鳞熊鳞片(scaly_bear_scale) 材料特性：<b>对友好生物造成的伤害 +20%（每级）</b>。
 * <p>
 * 「友好」沿用 J村 自己的阵营判定 {@link AttackFind#SameFactionAvoidDamage}——
 * 也就是同阵营、平时会互相避让不打的那些生物。如果以后想换口径
 * （例如改成 {@code AttackFind.FindCanNotAttack} 或某个具体阵营），只改下面这一处判断即可。
 * <p>
 * 数值走 TC 的近战伤害钩子，和 {@link AncientSecondRounderModifier} 同一个套路：
 * 先拿 {@code ToolAttackContext} 里的目标，判定通过后按等级乘算。
 */
public class ScalyBearScaleModifier extends Modifier implements MeleeDamageModifierHook {

    /** 每级对友好生物的额外伤害倍率 */
    private static final float BONUS_PER_LEVEL = 0.2f;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target == null || attacker == null || baseDamage <= 0 || damage <= 0) {
            return damage;
        }
        // 只对"友好生物"生效
        if (!AttackFind.SameFactionAvoidDamage(attacker, target, false)) {
            return damage;
        }
        float result = damage * (1.0f + BONUS_PER_LEVEL * modifier.getLevel());
        if (Float.isNaN(result) || Float.isInfinite(result)) {
            return damage;
        }
        return result;
    }
}
