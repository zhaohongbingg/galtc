package com.galtc2.jvt.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import javax.annotation.Nullable;

/**
 * 火力倾泻者·弩(firepower)
 * 复刻 J村 火力倾泻者弩：蓄力(拉弓/上弦)速度提升到 5 倍，且射出的弹射物无视目标的受击冷却(无敌帧)，
 * 对应水晶箭的 jerotes:bypasses_cooldown_shoot。
 * 占能力槽，可与其他强化共存。
 */
public class FirepowerModifier extends Modifier implements ToolStatsModifierHook, ProjectileHitModifierHook {

    /** 蓄力速度倍率(J村 5 tick vs 原版 25 tick = 5 倍) */
    private static final float DRAW_SPEED_MULTI = 5.0f;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        ToolStats.DRAW_SPEED.multiply(builder, DRAW_SPEED_MULTI);
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (target != null && target.isAlive()) {
            // 清掉受击冷却，让箭矢稳定结算(相当于 BYPASSES_COOLDOWN_SHOOT)
            target.invulnerableTime = 0;
        }
        return false;
    }
}
