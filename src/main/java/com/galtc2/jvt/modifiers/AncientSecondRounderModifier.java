package com.galtc2.jvt.modifiers;

import com.jerotes.jerotesvillage.util.OtherEntityFactionFind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.common.TinkerDamageTypes;
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

/**
 * 古二轮金属(ancient_second_rounder) 材料特性：复魂尸杀手。
 * 仿 J村 bane_of_humanoids：对沉降/复魂尸类生物( tag jerotes:resurrecter )追加伤害，
 * 近战公式 = damage + damage/baseDamage * 2.5 * 等级。
 * <p>
 * <b>箭矢分支（F-029）</b>：{@code MELEE_DAMAGE} 只对近战派发（{@code ToolAttackUtil} 入口的
 * {@code TinkerTags.Items.MELEE} 门槛），所以远程另挂 {@code PROJECTILE_HIT}。
 * 箭矢路径拿不到"本次最终伤害"，比例 {@code damage/baseDamage} 取 1，也就是
 * <b>每级追加 {@value #BONUS_PER_LEVEL} 点</b>（与近战同公式，只是比例按 1 处理）。
 * 追加伤害一律走 {@link ToolAttackUtil#attackEntitySecondary}（它会临时清空再还原
 * {@code invulnerableTime}），<b>不在这个 hook 里调 {@code target.hurt()}</b>，
 * 否则会把箭矢自身的伤害吃掉。
 * <p>
 * 去重：每支箭对同一目标只结算一次（箭矢 {@code persistentData} 打标记），
 * 穿射箭不会对同一个目标反复追加。
 */
public class AncientSecondRounderModifier extends Modifier implements MeleeDamageModifierHook, ProjectileHitModifierHook {

    private static final float BONUS_PER_LEVEL = 2.5f;

    /** 箭矢分支的去重标记（写在箭矢的 persistentData 上） */
    private static final ResourceLocation RANGED_DONE =
            ResourceLocation.fromNamespaceAndPath("galtc2", "ancient_second_rounder_ranged_done");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity target = context.getLivingTarget();
        if (target == null || baseDamage <= 0 || !OtherEntityFactionFind.isResurrecter(target.getType())) {
            return damage;
        }
        float result = damage + damage / baseDamage * BONUS_PER_LEVEL * modifier.getLevel();
        if (Float.isNaN(result) || Float.isInfinite(result)) {
            return 0.0f;
        }
        return result;
    }

    /**
     * 箭矢命中：对沉降/复魂尸额外补一次 +2.5/级的伤害。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         LivingEntity attacker, LivingEntity target) {
        // 服务端权威 + 空值/存活守卫
        if (projectile.level().isClientSide() || target == null || attacker == null || !target.isAlive()) {
            return false;
        }
        if (!(projectile instanceof AbstractArrow arrow)) {
            return false;
        }
        float extra = BONUS_PER_LEVEL * modifier.getLevel();
        if (extra <= 0) {
            return false;
        }
        // 只打沉降/复魂尸（沿用近战同一套判定）
        if (!OtherEntityFactionFind.isResurrecter(target.getType())) {
            return false;
        }
        // 每支箭对同一目标只算一次
        ResourceLocation key = rangedKey(target);
        if (persistentData.getBoolean(key)) {
            return false;
        }
        persistentData.putBoolean(key, true);

        DamageSource source = TinkerDamageTypes.source(
                projectile.level().registryAccess(), TinkerDamageTypes.MELEE_ARROW, arrow, attacker);
        ToolAttackUtil.attackEntitySecondary(source, extra, target, target, true);
        return false;
    }

    /** 去重键 = 固定标记 + 命中目标 id（穿射同一支箭打不同目标时各自结算一次） */
    private static ResourceLocation rangedKey(LivingEntity target) {
        return ResourceLocation.fromNamespaceAndPath("galtc2", RANGED_DONE.getPath() + "_" + target.getId());
    }
}
