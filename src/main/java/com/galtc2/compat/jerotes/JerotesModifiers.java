package com.galtc2.compat.jerotes;

import com.galtc2.Galtc2;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

/**
 * J村联动强化/特性的注册位。
 * <p>
 * 本类只在 J村 存在时由 {@link JerotesCompat#init} 加载并注册，
 * 因此这里可以安全地引用 J村 API。
 */
public final class JerotesModifiers {

    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Galtc2.MODID);

    /** 卷轴·法术刻印：用"无暇魔法卷轴"把 J村 法术刻印进工具。 */
    public static final StaticModifier<ScrollCastModifier> scrollCast =
            MODIFIERS.register("scroll_cast", ScrollCastModifier::new);

    /** 火焰使徒：每件护甲降低火系承伤、提升自身火系输出（复用 J村 火系判定）。原词条保留注册。 */
    public static final StaticModifier<FireApostleModifier> fireApostle =
            MODIFIERS.register("fire_apostle", FireApostleModifier::new);
    /** 火焰使徒·炎征(攻击侧): 只提升火系输出。 */
    public static final StaticModifier<FireApostleMeleeModifier> fireApostleMelee =
            MODIFIERS.register("fire_apostle_melee", FireApostleMeleeModifier::new);
    /** 火焰使徒·炎护(防御侧): 只降低火系承伤。 */
    public static final StaticModifier<FireApostleArmorModifier> fireApostleArmor =
            MODIFIERS.register("fire_apostle_armor", FireApostleArmorModifier::new);

    private JerotesModifiers() {
    }
}
