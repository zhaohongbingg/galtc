package com.galtc2.compat.bomd.modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

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
 */
public class SoulHeartModifier extends Modifier implements MeleeDamageModifierHook, ModifyDamageModifierHook {

    /** 每级的增伤 / 减伤比例 */
    private static final float BONUS_PER_LEVEL = 0.25f;

    public SoulHeartModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.MODIFY_HURT);
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
