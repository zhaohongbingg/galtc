package com.galtc2.jvt.modifiers;

import com.galtc2.compat.jerotes.FireAffinityModifier;

/**
 * 蛇龙强者·极炎（攻击侧）。
 * <p>
 * 只提升火系输出，不再降低火系承伤；前置材料校验（{@code VALIDATE} 钩子）随本类保留。
 */
public class SerponPowerhouseMeleeModifier extends SerponPowerhouseModifier {

    @Override
    public float fireTakenReduction() {
        return 0.0f;
    }
}
