package com.galtc2.jvt.modifiers;

import com.galtc2.jvt.Register.JvtModifier;
import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** 武器能力强化的互斥判定。 */
public final class WeaponEnhancements {
    private WeaponEnhancements() {
    }

    /** 互为排斥的能力强化（疯狂旋风 / 不祥破盾 已删除，不再参与判定） */
    private static final ModifierId[] GROUP = {
            JvtModifier.CHAINSAW,
            JvtModifier.SEAWAVE_TRIDENT,
            JvtModifier.DROUGHT_WILDFIRE_SHIELD
    };

    /**
     * 若工具上已存在本组中的其它强化，返回"不兼容其他强化"错误，否则返回 null。
     * 用于 {@code ValidateModifierHook}。
     */
    public static Component validateExclusive(IToolStackView tool, ModifierEntry self) {
        for (ModifierId id : GROUP) {
            if (!id.equals(self.getId()) && tool.getModifierLevel(id) > 0) {
                return Component.translatable("modifier.galtc2.incompatible");
            }
        }
        return null;
    }
}
