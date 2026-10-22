package com.galtc2.jvt.modifiers;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 明路之心(bright_land_heart)
 * 佩戴时提升最大生命值(每件 +4)。
 * 无槽位、不互斥。
 * <p>
 * 每个装备槽位用各自的 UUID：实体属性按 UUID 去重，4 件护甲共用同一个 UUID 时
 * 后面的会把前面的顶掉（甚至整套装备属性更新失败），表现为"加了但没效果"。
 * 原版 {@code ArmorItem} 与匠魂 {@code ModifiableArmorItem} 也是按槽位分配 UUID 的。
 */
public class BrightLandHeartModifier extends Modifier implements AttributesModifierHook {

    private static final double MAX_HEALTH_PER_LEVEL = 4.0;

    /** 下标 = {@link EquipmentSlot#getIndex()}（主手 0 / 副手 1 / 靴 2 / 腿 3 / 胸 4 / 头 5） */
    private static final UUID[] HEALTH_UUIDS = new UUID[6];

    static {
        for (int i = 0; i < HEALTH_UUIDS.length; i++) {
            // 基础 UUID 末位按槽位递增，保证 6 个槽位互不冲突
            HEALTH_UUIDS[i] = new UUID(0x5e2f7c1a3b6d4a8eL, 0x9c1f2d4b6a8c0e10L + i);
        }
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ATTRIBUTES);
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Attribute, AttributeModifier> consumer) {
        if (slot.getType() != EquipmentSlot.Type.ARMOR) {
            return;
        }
        consumer.accept(Attributes.MAX_HEALTH, new AttributeModifier(HEALTH_UUIDS[slot.getIndex()],
                "jvt.bright_land_heart", MAX_HEALTH_PER_LEVEL * modifier.getLevel(), AttributeModifier.Operation.ADDITION));
    }
}
