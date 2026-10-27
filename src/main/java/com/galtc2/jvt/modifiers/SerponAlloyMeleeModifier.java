package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.util.AttackFind;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

/**
 * 蛇龙合金·龙炎（攻击侧）。
 * <p>
 * 只提升火系输出，不再降低火系承伤（把 {@link com.galtc2.compat.jerotes.FireAffinityModifier#fireTakenReduction()} 归零）。
 * 近战侧的「+15% 火系输出/级」（{@code SerponAlloyModifier#fireDealtBonus()}）由 {@code JerotesEvents} 全局统计，
 * <b>本类不改这条数值</b>（{@code serpon_alloy} 与 {@code fire_apostle} 共用同一个 trait id）。
 * <p>
 * <b>箭矢分支（F-029，只对火焰使徒所在的远程组生效）</b>：
 * <ul>
 *   <li>命中给目标点燃 {@value #FIRE_SECONDS_PER_LEVEL} 秒 × 等级（照匠魂 {@code FieryAttackModule} 的口径，
 *       直接 {@code setSecondsOnFire}）；</li>
 *   <li>追加一次<b>等量于该箭 {@code baseDamage}</b> 的火焰伤害，也就是"这一箭的火焰伤害合计 ×2"；
 *       追加伤害走 {@link ToolAttackUtil#attackEntitySecondary}（会还原 {@code invulnerableTime}），
 *       严禁在 {@code PROJECTILE_HIT} 里调 {@code target.hurt()}。</li>
 * </ul>
 * 伤害类型用 J村 的射击档火伤 {@code jerotes:fire_shoot}（在 {@code minecraft:is_fire} 标签内，
 * 所以 {@code JerotesEvents} 的火系输出加成同样会作用于这部分伤害）；取不到时退化到匠魂通用来源。
 * <p>
 * 每支箭对同一目标只追加一次（箭矢 {@code persistentData} 标记）；附火本身是幂等的，按命中逐个目标施加。
 */
public class SerponAlloyMeleeModifier extends SerponAlloyModifier implements ProjectileHitModifierHook {

    /** 远程附火：每级点燃秒数（匠魂 fiery 口径 = 5 秒/级） */
    private static final int FIRE_SECONDS_PER_LEVEL = 5;

    /** J村 射击档火焰伤害（在 minecraft:is_fire tag 内） */
    private static final ResourceKey<DamageType> FIRE_SHOOT =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("jerotes", "fire_shoot"));

    /** 箭矢分支的去重标记（写在箭矢的 persistentData 上） */
    private static final ResourceLocation RANGED_DONE =
            ResourceLocation.fromNamespaceAndPath("galtc2", "serpon_alloy_melee_ranged_done");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        // 父类挂 MODIFY_HURT（护甲侧减伤），这里追加箭矢命中
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public float fireTakenReduction() {
        return 0.0f;
    }

    /**
     * 箭矢命中：附火 + 追加等量火焰伤害（合计约 ×2）。
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
        int level = modifier.getLevel();
        if (level <= 0) {
            return false;
        }
        // 附火：幂等，按命中目标逐个施加（与匠魂 fiery 一致）
        target.setSecondsOnFire(FIRE_SECONDS_PER_LEVEL * level);

        float extra = (float) arrow.getBaseDamage();
        if (extra <= 0 || Float.isNaN(extra) || Float.isInfinite(extra)) {
            return false;
        }
        // 追加火焰伤害：每支箭对同一目标只算一次
        ResourceLocation key = rangedKey(target);
        if (persistentData.getBoolean(key)) {
            return false;
        }
        persistentData.putBoolean(key, true);

        ToolAttackUtil.attackEntitySecondary(fireSource(projectile, attacker), extra, target, target, true);
        return false;
    }

    /**
     * 取火焰伤害源：优先 J村 射击档 {@code jerotes:fire_shoot}（火系加成才认），
     * 取不到时退化到匠魂通用来源（仍会造成伤害，只是不再算"火系"）。
     */
    private static DamageSource fireSource(Projectile projectile, LivingEntity attacker) {
        Level level = projectile.level();
        try {
            return AttackFind.findDamageType(projectile, FIRE_SHOOT, projectile, attacker);
        } catch (RuntimeException ignored) {
            return TinkerDamageTypes.source(level.registryAccess(), TinkerDamageTypes.MELEE_ARROW, projectile, attacker);
        }
    }

    /** 去重键 = 固定标记 + 命中目标 id（穿射同一支箭打不同目标时各自结算一次） */
    private static ResourceLocation rangedKey(LivingEntity target) {
        return ResourceLocation.fromNamespaceAndPath("galtc2", RANGED_DONE.getPath() + "_" + target.getId());
    }
}
