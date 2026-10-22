package com.galtc2.compat.relicward.modifiers;

import cn.suiyi.relicward.combat.ArmorFracture;
import com.galtc2.modifiers.BaseModifiers;
import com.galtc2.modifiers.TraitLevels;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

/**
 * 裂甲 (armor_fracture) —— 遗境守望 联动特性。
 * <p>
 * 近战命中或箭矢命中时，给目标打上遗境守望 自己的「裂甲」
 * （{@link ArmorFracture}）：<b>护甲与护甲韧性各 −30%</b>（{@code MULTIPLY_TOTAL}），
 * 就是玄铎的钟波、回响钟锤的震击、精准格挡反震打在人身上的那个东西。
 * 这里不自己造破甲效果，直接调模组的 {@link ArmorFracture#apply(LivingEntity, int)}，
 * 表现（HUD 图标、语言键 {@code effect.relicward.armor_fracture}）与遗境守望 完全一致。
 * <p>
 * 等级口径沿用 {@link TraitLevels}（和清枫 / 莫尔加那 一样）：
 * <ul>
 *   <li><b>近战</b>：必须手持带本特性的武器，等级 = 手上等级 + 4 件盔甲之和 / 2。
 *       盔甲上也会收到匠魂的近战命中钩子，所以不能拿钩子传进来的等级当武器等级；</li>
 *   <li><b>远程</b>：等级取弓弩自身（箭矢里存的强化数据）+ 盔甲加成。</li>
 * </ul>
 * 时长随等级成长：1 级 {@value #BASE_DURATION} tick（5 秒，与回响钟锤震击同口径），
 * 之后每级 +{@value #DURATION_PER_LEVEL} tick。
 * <p>
 * <b>为什么还要一段内部冷却</b>：模组里裂甲是挂在有冷却的招式上的，
 * 而特性是"每刀都触发"，不去限频的话等于给目标挂了个永久破甲。
 * 这里用一个很短的 {@value #ATTACK_COOLDOWN} tick 闸门，效果是"持续输出就维持住，
 * 收刀就按时失效"，不会因为一次挥砍被别的钩子吃掉。
 */
public class ArmorFractureModifier extends BaseModifiers {

    /** 1 级裂甲时长（tick）：与回响钟锤震击的 100 tick 一致 */
    private static final int BASE_DURATION = 100;
    /** 每高 1 级多出来的裂甲时长（tick） */
    private static final int DURATION_PER_LEVEL = 40;
    /** 触发闸门（tick）：避免每次挥砍都去刷新效果 */
    private static final long ATTACK_COOLDOWN = 20L;
    /** 闸门时间戳写在实体自己身上（与清枫 / 莫尔加那 同一套做法） */
    private static final String KEY_COOLDOWN = "galtc2_relicward_fracture";

    public ArmorFractureModifier() {
    }

    @Override
    public void onAfterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                               LivingEntity attacker, LivingEntity target, float damageDealt) {
        if (damageDealt <= 0) {
            return;
        }
        int level = TraitLevels.weaponLevel(attacker, this.getId());
        if (level <= 0) {
            return;
        }
        trigger(attacker, target, level);
    }

    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level,
                                      Projectile projectile, AbstractArrow arrow, EntityHitResult hit,
                                      LivingEntity attacker, LivingEntity target) {
        trigger(attacker, target, level + TraitLevels.armorLevel(attacker, this.getId()) / TraitLevels.ARMOR_LEVEL_DIVISOR);
    }

    private static void trigger(LivingEntity attacker, LivingEntity target, int level) {
        if (target == null || level <= 0) {
            return;
        }
        long now = attacker.level().getGameTime();
        if (now - attacker.getPersistentData().getLong(KEY_COOLDOWN) < ATTACK_COOLDOWN) {
            return;
        }
        attacker.getPersistentData().putLong(KEY_COOLDOWN, now);
        ArmorFracture.apply(target, duration(level));
    }

    /** 等级对应的裂甲时长（tick）：1 级 5 秒，之后每级 +2 秒 */
    public static int duration(int level) {
        return BASE_DURATION + DURATION_PER_LEVEL * (Math.max(1, level) - 1);
    }
}
