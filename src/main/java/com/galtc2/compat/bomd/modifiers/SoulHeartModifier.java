package com.galtc2.compat.bomd.modifiers;

import com.galtc2.Galtc2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;

/**
 * 魂人 (soul_heart) —— 祸乱鬼魅 联动材料特性。
 * <p>
 * 每级：
 * <ul>
 *   <li>对<b>亡灵</b>（原版 {@code minecraft:undead} 标签）与<b>暗夜巫妖</b>（BOMD 的
 *       {@code bosses_of_mass_destruction:lich}）造成的近战伤害 <b>+25%</b>；</li>
 *   <li>受到来自它们的伤害 <b>−25%</b>。</li>
 * </ul>
 * 攻击侧走 {@code MELEE_DAMAGE}；防御侧走 {@code MODIFY_HURT}，
 * 后者对每件带该特性的护甲各调一次，所以要先确认由哪一件结算，否则四件护甲会叠成 −100%。
 * <p>
 * <b>远程</b>（F-029 新增）：箭矢命中走 {@code PROJECTILE_HIT}，目标同样是亡灵/暗夜巫妖时补
 * {@code baseDamage × 0.25 × 等级}（与近战同比例），走 {@code attackEntitySecondary}，
 * 并用箭矢 persistentData {@value #KEY_ARROW_DONE} 保证<b>每支箭只结算一次</b>（穿射不滚雪球）。
 * 防御侧的 {@code MODIFY_HURT} 与弓无关，保持不动。
 */
public class SoulHeartModifier extends Modifier
        implements MeleeDamageModifierHook, ModifyDamageModifierHook, ProjectileHitModifierHook {

    /** 每级的增伤 / 减伤比例 */
    private static final float BONUS_PER_LEVEL = 0.25f;
    /** 箭矢去重键：一支箭只结算一次。写在 projectile 的 TConstruct 持久数据里 */
    private static final ResourceLocation KEY_ARROW_DONE = Galtc2.loc("soul_heart_done");

    public SoulHeartModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.MODIFY_HURT, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                float baseDamage, float damage) {
        if (!isSoulFoe(context.getLivingTarget())) {
            return damage;
        }
        return damage * (1.0f + BONUS_PER_LEVEL * modifier.getEffectiveLevel());
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                                   EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0.0f) {
            return amount;
        }
        // 只让固定顺序上的第一件护甲结算
        if (slotType != BomdHelper.firstArmorSlot(context, this)) {
            return amount;
        }
        if (!isSoulFoe(source.getEntity())) {
            return amount;
        }
        float result = amount * (1.0f - BONUS_PER_LEVEL * modifier.getEffectiveLevel());
        return Math.max(0.0f, result);
    }

    /** 远程：箭矢命中亡灵/暗夜巫妖时补一次 {@code baseDamage × 0.25 × 等级} 的伤害 */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null || !target.isAlive() || !isSoulFoe(target)) {
            return false;
        }
        if (projectile.level().isClientSide() || !(projectile instanceof AbstractArrow arrow)) {
            return false;
        }
        // 注意 ModifierEntry#getEffectiveLevel() 返回 float，别用 int 接（会因精度丢失编不过）
        float level = modifier.getEffectiveLevel();
        if (level <= 0.0f) {
            return false;
        }
        // 每支箭只结算一次（倍率型必须去重）
        if (persistentData.getBoolean(KEY_ARROW_DONE)) {
            return false;
        }
        persistentData.putBoolean(KEY_ARROW_DONE, true);
        float baseDamage = (float) arrow.getBaseDamage();
        if (baseDamage <= 0.0f) {
            return false;
        }
        DamageSource source = attacker.damageSources().arrow(arrow, attacker);
        ToolAttackUtil.attackEntitySecondary(source, baseDamage * BONUS_PER_LEVEL * level, target, target, true);
        return false;
    }

    /** 亡灵 或 暗夜巫妖（BOMD） */
    private static boolean isSoulFoe(Entity entity) {
        if (entity == null) {
            return false;
        }
        // 1.20.1 没有 EntityTypeTags.UNDEAD 这个常量，原版判亡灵走 MobType
        // （亡灵杀手附魔用的也是它），这里照同一套来。
        if (entity instanceof LivingEntity living && living.getMobType() == MobType.UNDEAD) {
            return true;
        }
        return BomdHelper.isNightLich(entity);
    }
}
