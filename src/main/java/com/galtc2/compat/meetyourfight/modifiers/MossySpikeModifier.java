package com.galtc2.compat.meetyourfight.modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Objects;

/**
 * 藓牙牙刺 (mossy_spike) —— 迎战 联动特性：藓牙 材料的攻击侧特性。
 * <p>
 * 对应迎战 里用藓牙 做的"骨质钉耙"（{@code boneRakerBonus = 2.0}）：额外 <b>+2 穿甲伤害</b>。
 * <p>
 * 穿甲部分完全复用匠魂自带「穿甲(Piercing)」升级的做法：在 {@code MELEE_HIT} 之后用
 * {@link TinkerDamageTypes#PIERCING}（自带 {@code bypasses_armor} 标签）补一次
 * {@link ToolAttackUtil#attackEntitySecondary} 结算 —— 真穿甲，不会撞普通无敌帧。
 */
public class MossySpikeModifier extends Modifier implements MeleeHitModifierHook {

    /** 每级穿甲伤害 */
    private static final float PIERCE_DAMAGE = 2f;

    public MossySpikeModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (context.getLevel().isClientSide()) {
            return;
        }
        LivingEntity target = context.getLivingTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        DamageSource source = TinkerDamageTypes.source(
                context.getLevel().registryAccess(),
                TinkerDamageTypes.PIERCING,
                Objects.requireNonNullElse(context.getPlayerAttacker(), context.getAttacker()));
        ToolAttackUtil.attackEntitySecondary(source, PIERCE_DAMAGE * modifier.getEffectiveLevel(),
                context.getTarget(), target, true);
    }
}
