package com.galtc2.jvt.modifiers;

import com.galtc2.compat.jerotes.FireAffinityModifier;

/**
 * 蛇龙合金(serpon_alloy) 材料特性。
 * 每件降低 10% 火系承伤、提升 15% 火系输出（火焰使徒的一半）。
 */
public class SerponAlloyModifier extends FireAffinityModifier {

    @Override
    public float fireTakenReduction() {
        return 0.10f;
    }

    @Override
    public float fireDealtBonus() {
        return 0.15f;
    }
}
