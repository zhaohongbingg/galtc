package com.galtc2.jvt.modifiers;

import com.galtc2.compat.jerotes.FireAffinityModifier;

/**
 * 蛇龙合金·龙鳞（防御侧）。
 * <p>
 * 只降低火系承伤，不再提升火系输出（把 {@link FireAffinityModifier#fireDealtBonus()} 归零）。
 */
public class SerponAlloyArmorModifier extends SerponAlloyModifier {

    @Override
    public float fireDealtBonus() {
        return 0.0f;
    }
}
