package com.galtc2.modifiers;

import com.galtc2.GaltcConfig;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class NightTimeSheep extends BaseModifiers {
    public NightTimeSheep() {
    }

    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        // 阈值每次实时读取：原来用 static final 在类加载时就取了一次，
        // 改配置后不重启不生效，且可能早于 config 初始化拿到默认值。
        float threshold = GaltcConfig.COMMON.ATTACK_THRESHOLD.get().floatValue();
        if (threshold <= 0.0F || !Float.isFinite(damage) || damage <= 0.0F) {
            return damage;
        }

        // 低于阈值才加成：damage * (阈值 - damage)，阈值一半处最高（小才是好）。
        // 原式在 damage 接近阈值时会低于原伤害（阈值 18 时 17.5 只剩 8.75），
        // 这里兜回原伤害，保证这条特性只会增伤、不会减伤。
        float boosted = damage * (threshold - damage);
        return Math.max(damage, boosted);
    }
}
