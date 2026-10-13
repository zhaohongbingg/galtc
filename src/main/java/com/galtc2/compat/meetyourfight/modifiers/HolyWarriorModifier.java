package com.galtc2.compat.meetyourfight.modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 圣战士 (holy_warrior) —— 迎战 联动特性：命运之眷 材料的防御侧特性。
 * <p>
 * 对应迎战 里用命运之眷 做的"铁之王牌"：<b>每件护甲基础 1/6 概率完全免疫这次伤害</b>，
 * 概率受<b>受击方的幸运值</b>影响（照抄迎战的 aceOfIronLuck：正幸运 {@code (1+L)/((1/P)+2L)}
 * 且只有基础概率 &lt; 0.5 时才生效，负幸运 {@code 1/((1/P)-3L)}；正幸运的渐近上限是 50%）。
 * <p>
 * 匠魂会把 {@code MODIFY_HURT} 对每件带该特性的护甲各调一次，所以这里先数出穿了几件，
 * 先在「单件概率」上套幸运公式，再按"各件独立"折算成一次判定（{@code 1-(1-p)^n}），
 * 并且只在第一件护甲的钩子里结算。
 */
public class HolyWarriorModifier extends Modifier implements ModifyDamageModifierHook {

    /** 每件护甲的基础免疫概率 */
    private static final double CHANCE_PER_PIECE = 1.0 / 6.0;
    /** 迎战 aceOfIronLuck 的系数：正幸运 分子1/分母2，负幸运 3 */
    private static final double LUCK_POSITIVE_NUMERATOR = 1.0;
    private static final double LUCK_POSITIVE_DENOMINATOR = 2.0;
    private static final double LUCK_NEGATIVE = 3.0;

    public HolyWarriorModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MODIFY_HURT);
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                                   EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0) {
            return amount;
        }
        int pieces = MeetYourFightHelper.armorCount(context, this);
        if (pieces == 0 || slotType != MeetYourFightHelper.firstArmorSlot(context, this)) {
            return amount;
        }
        LivingEntity wearer = context.getEntity();
        if (wearer == null) {
            return amount;
        }

        // 单件概率：先套幸运公式（迎战 里"基础概率 >= 0.5 时正幸运不生效"，这里照抄）
        double perPieceChance = CHANCE_PER_PIECE;
        float luck = MeetYourFightHelper.luckOf(wearer);
        if (luck >= 0) {
            if (perPieceChance < 0.5) {
                perPieceChance = MeetYourFightHelper.luckChance(perPieceChance, luck,
                        LUCK_POSITIVE_NUMERATOR, LUCK_POSITIVE_DENOMINATOR, LUCK_NEGATIVE);
            }
        } else {
            perPieceChance = MeetYourFightHelper.luckChance(perPieceChance, luck,
                    LUCK_POSITIVE_NUMERATOR, LUCK_POSITIVE_DENOMINATOR, LUCK_NEGATIVE);
        }

        // 多件护甲 = 多次独立判定，折算成一次掷骰
        double chance = 1.0 - Math.pow(1.0 - perPieceChance, pieces);
        if (wearer.getRandom().nextDouble() < chance) {
            return 0f;
        }
        return amount;
    }
}
