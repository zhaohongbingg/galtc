package com.galtc2.jvt.modifiers;

/**
 * 蛇龙强者·极御（防御侧）。
 * <p>
 * 只降低火系承伤，不再提升火系输出；前置材料校验（{@code VALIDATE} 钩子）随本类保留。
 */
public class SerponPowerhouseArmorModifier extends SerponPowerhouseModifier {

    @Override
    public float fireDealtBonus() {
        return 0.0f;
    }
}
