package com.galtc2.modifiers;

import com.galtc2.GaltcConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

public class NightTimeSheep extends BaseModifiers {
    public NightTimeSheep() {
    }

    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        // 阈值每次实时读取：原来用 static final 在类加载时就取了一次，
        // 改配置后不重启不生效，且可能早于 config 初始化拿到默认值。
        float threshold = GaltcConfig.COMMON.ATTACK_THRESHOLD.get().floatValue();
        if (threshold <= 0.0F || !Float.isFinite(damage) || damage <= 0.0F) {
            return damage;
        }

        // 低于阈值才加成：damage * (阈值 - damage)，阈值一半处最高（小才是好）。
        // 原式在 damage 接近阈值时会低于原伤害（阈值 18 时 17.5 只剩 8.75），
        // 这里兜回原伤害，保证这条特性只会增伤、不会减伤。
        float boosted = damage * (threshold - damage);
        return Math.max(damage, boosted);
    }

    /**
     * 箭矢命中：同一套阈值公式，但<b>以该箭的 baseDamage 为口径</b>，并且<b>追加伤害封顶 = baseDamage</b>
     * （即最多翻倍）。近战原式在两个阈值一般大小的伤害上会爆出很大的数（18 阈值下 9 点能加成 81 点），
     * 弓的 baseDamage 天生就低、且没有"贴脸风险"，所以远程单独夹一个 100% 上限。
     * <p>
     * 追加伤害走 {@code attackEntitySecondary}（不碰 {@code target.hurt}，避免吞掉箭矢自身伤害）。
     */
    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level,
                                      Projectile projectile, AbstractArrow arrow, EntityHitResult hit,
                                      LivingEntity attacker, LivingEntity target) {
        if (arrow == null || attacker == null || target == null) return;
        if (target.level().isClientSide() || target.isDeadOrDying()) return;

        float base = (float) arrow.getBaseDamage();
        float threshold = GaltcConfig.COMMON.ATTACK_THRESHOLD.get().floatValue();
        if (threshold <= 0.0F || base <= 0.0F || !Float.isFinite(base)) return;

        float bonus = base * (threshold - base);
        if (bonus <= 0.0F) return;
        // 封顶 = 该箭 baseDamage（最多翻倍）
        bonus = Math.min(bonus, base);

        ToolAttackUtil.attackEntitySecondary(attacker.damageSources().arrow(arrow, attacker),
                bonus, target, target, true);
    }
}
