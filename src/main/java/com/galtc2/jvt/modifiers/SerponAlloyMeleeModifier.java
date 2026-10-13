package com.galtc2.jvt.modifiers;

import com.galtc2.compat.jerotes.FireAffinityModifier;

/**
 * 蛇龙合金·龙炎（攻击侧）。
 * <p>
 * 只提升火系输出，不再降低火系承伤（把 {@link FireAffinityModifier#fireTakenReduction()} 归零）。
 */
public class SerponAlloyMeleeModifier extends SerponAlloyModifier {

    @Override
    public float fireTakenReduction() {
        return 0.0f;
    }
}
