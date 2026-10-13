package com.galtc2.compat.jerotes;

/**
 * 火焰使徒·炎征（攻击侧）。
 * <p>
 * 只提升火系输出，不再降低火系承伤。
 * 注意：火系攻击加成不是 ModifierHook，而是由 {@link JerotesEvents} 读取
 * {@link FireAffinityModifier#fireDealtBonus()} 实现的，所以这里把防御侧系数归零即可。
 */
public class FireApostleMeleeModifier extends FireApostleModifier {

    @Override
    public float fireTakenReduction() {
        return 0.0f;
    }
}
