package com.galtc2.compat.relicward.modifiers;

import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 钟甲 (bell_ward) —— 遗境守望 联动防御特性，挂在 钟铸铜板 材料上。
 * <p>
 * 两条效果：
 * <ul>
 *   <li><b>每级 +5% 抗击退</b>：走 {@link Attributes#KNOCKBACK_RESISTANCE} 属性修饰符
 *       （{@code ADDITION}，每件带该特性的护甲各算一次；4 件 1 级 = 20%）。
 *       UUID 固定，避免同一件装备反复重算时叠出多份；</li>
 *   <li><b>单次 6 点以上的伤害减免 30%</b>：门槛与比例都是常量，写在
 *       {@code MODIFY_HURT} 钩子里（同匠魂的"承重/护盾"类特性写法）。
 *       低于门槛的小刀小砍一分不减 —— 这特性管的是"挨一下狠的"。</li>
 * </ul>
 * 护甲钩子对每件带该特性的护甲各调一次，所以减伤只让固定槽位顺序（脚→腿→胸→头）上的
 * 第一件负责结算，否则穿 4 件会结算 4 次（{@link RelicWardHelper#firstArmorSlot}）。
 */
public class BellWardModifier extends BaseModifiers {

    /** 每级抗击退（5%） */
    private static final float KNOCKBACK_RESISTANCE_PER_LEVEL = 0.05f;
    /** 触发减伤的单次伤害门槛（点） */
    private static final float HEAVY_HIT_THRESHOLD = 6.0f;
    /** 达到门槛后的减免比例（30%） */
    private static final float HEAVY_HIT_REDUCTION = 0.3f;
    /** 抗击退属性修饰符 UUID：固定值，保证同一件护甲只有一份加成 */
    private static final UUID KNOCKBACK_UUID = UUID.fromString("7f3c1a94-2d64-4b18-9ae5-0c5b7d19f2a8");

    public BellWardModifier() {
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot,
                              BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(KNOCKBACK_UUID, "galtc2:bell_ward_knockback",
                        KNOCKBACK_RESISTANCE_PER_LEVEL * modifier.getEffectiveLevel(),
                        AttributeModifier.Operation.ADDITION));
    }

    @Override
    public float onModifyTakeDamage(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                                    EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        if (amount < HEAVY_HIT_THRESHOLD) {
            return amount;
        }
        // 只让固定顺序上的第一件护甲结算
        if (slotType != RelicWardHelper.firstArmorSlot(context, this)) {
            return amount;
        }
        return amount * (1.0f - HEAVY_HIT_REDUCTION);
    }
}
