package com.galtc2.compat.meetyourfight.modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 藓牙牙刺 (mossy_spike) —— 迎战 联动特性：藓牙 材料的攻击侧特性。
 * <p>
 * 对应迎战 里用藓牙 做的"骨质钉耙"（{@code boneRakerBonus = 2.0}）：额外 <b>+2 穿甲伤害/级</b>。
 * <p>
 * 穿甲部分完全复用匠魂自带「穿甲(Piercing)」升级的做法：在 {@code MELEE_HIT} 之后用
 * {@link TinkerDamageTypes#PIERCING}（自带 {@code bypasses_armor} 标签）补一次
 * {@link ToolAttackUtil#attackEntitySecondary} 结算 —— 真穿甲，不会撞普通无敌帧。
 * <p>
 * <b>远程</b>（F-029 新增）：箭矢命中走 {@code PROJECTILE_HIT}，同样补 {@code 2 × 等级} 的穿甲伤害，
 * 伤害源照近战用 {@link TinkerDamageTypes#PIERCING}，结算走 {@code attackEntitySecondary}。
 * 这是<b>固定加值</b>（不是倍率/掷骰），所以每个被这支箭命中的目标各结算一次即可 ——
 * 不需要"每支箭一次"的标记（穿射命中 3 个目标 = 3 个目标各被穿甲一次，与近战连砍 3 个一致）。
 */
public class MossySpikeModifier extends Modifier implements MeleeHitModifierHook, ProjectileHitModifierHook {

    /** 每级穿甲伤害 */
    private static final float PIERCE_DAMAGE = 2f;

    public MossySpikeModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.PROJECTILE_HIT);
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

    /** 远程：箭矢命中后补一次固定穿甲伤害（与近战同数值、同伤害类型） */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null || !target.isAlive()) {
            return false;
        }
        if (projectile.level().isClientSide()) {
            return false;
        }
        // 注意 ModifierEntry#getEffectiveLevel() 返回 float，别用 int 接（会因精度丢失编不过）
        float level = modifier.getEffectiveLevel();
        if (level <= 0.0f) {
            return false;
        }
        DamageSource source = TinkerDamageTypes.source(
                projectile.level().registryAccess(),
                TinkerDamageTypes.PIERCING,
                attacker);
        ToolAttackUtil.attackEntitySecondary(source, PIERCE_DAMAGE * level, target, target, true);
        return false;
    }
}
