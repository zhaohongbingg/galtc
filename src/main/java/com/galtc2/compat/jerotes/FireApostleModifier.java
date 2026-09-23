package com.galtc2.compat.jerotes;

/**
 * 火焰使徒 (fire_apostle) —— J村火系材料特性。
 * 每件护甲降低 20% 火系承伤、提升 30% 火系输出。
 */
public class FireApostleModifier extends FireAffinityModifier {

    @Override
    public float fireTakenReduction() {
        return 0.20f;
    }

    @Override
    public float fireDealtBonus() {
        return 0.30f;
    }
}
