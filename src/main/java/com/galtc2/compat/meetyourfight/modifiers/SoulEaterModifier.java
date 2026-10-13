package com.galtc2.compat.meetyourfight.modifiers;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 魂喰 (soul_eater) —— 迎战(Meet Your Fight) 联动特性：幽魂质 材料。
 * <p>
 * 两条效果分别对应迎战 里用幽魂质 做的两件道具：
 * <ul>
 *   <li><b>+2 方块触及距离</b>（对应"幽灵之握"）：复用匠魂自带 {@code tconstruct:reach} 的做法，
 *       直接给 {@link ForgeMod#BLOCK_REACH} 加属性，握在手里/穿在身上都生效；</li>
 *   <li><b>对发光生物造成额外伤害</b>（对应"幽灵之眼"）：近战目标身上有 {@code minecraft:glowing}
 *       时加伤，判定写在 {@code MELEE_DAMAGE} 钩子里（同匠魂的"导电"特性写法）。</li>
 * </ul>
 */
public class SoulEaterModifier extends Modifier implements AttributesModifierHook, MeleeDamageModifierHook {

    /** 方块触及距离加成（格/级） */
    private static final int REACH_BONUS = 2;
    /** 对发光生物的额外伤害比例（0.5 = +50%） */
    private static final float GLOWING_DAMAGE_BONUS = 0.5f;
    /** 属性修饰符 UUID：固定值，避免同一件装备被重复叠 */
    private static final UUID REACH_UUID = UUID.fromString("8b1f4c52-5a2f-4d6e-9c3a-1d7f0b6e2a41");

    public SoulEaterModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ATTRIBUTES, ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot,
                              BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(ForgeMod.BLOCK_REACH.get(),
                new AttributeModifier(REACH_UUID, "galtc2:soul_eater_reach", REACH_BONUS * modifier.getEffectiveLevel(),
                        AttributeModifier.Operation.ADDITION));
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                float baseDamage, float damage) {
        // 只在服务端掷骰/判定，客户端预测走原伤害
        if (context.getAttacker().level().isClientSide()) {
            return damage;
        }
        LivingEntity target = context.getLivingTarget();
        if (target != null && target.hasEffect(MobEffects.GLOWING)) {
            return damage * (1f + GLOWING_DAMAGE_BONUS * modifier.getEffectiveLevel());
        }
        return damage;
    }
}
