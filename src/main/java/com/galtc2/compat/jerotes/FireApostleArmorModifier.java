package com.galtc2.compat.jerotes;

/**
 * 火焰使徒·炎护（防御侧）。
 * <p>
 * 只降低火系承伤，不再提升火系输出（把 {@code fireDealtBonus()} 归零，
 * 于是 {@link JerotesEvents} 读到的加成为 0）。
 */
public class FireApostleArmorModifier extends FireApostleModifier {

    @Override
    public float fireDealtBonus() {
        return 0.0f;
    }
}
