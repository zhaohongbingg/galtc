package com.galtc2.compat.aetherbedrock;

import com.galtc2.Galtc2;
import com.galtc2.compat.aetherbedrock.modifiers.GolemCageModifier;
import com.galtc2.compat.aetherbedrock.modifiers.GolemSpikeChargeModifier;
import com.galtc2.compat.aetherbedrock.modifiers.NagaQuakeModifier;
import com.galtc2.compat.aetherbedrock.modifiers.SpiritSwordfallModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

/**
 * 天境基石生物重制(Aether Bedrock Mobs Reborn) 联动强化的注册位，
 * 写法与 遗境守望 的 {@code RelicWardModifiers}、祸乱鬼魅 的 {@code BomdModifiers} 一致。
 * <p>
 * 本类只在装了 Boss 包时由 {@link AetherBedrockCompat#init} 加载并注册，
 * 下面的强化类只引用原版 API（不 import 任何 {@code com.aetherbedrockmobsreborn.*} 类），
 * 所以即使原模组不在也不会 NoClassDefFoundError —— 配方侧仍用
 * {@code forge:mod_loaded} 条件兜底。
 */
public final class AetherBedrockModifiers {

    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Galtc2.MODID);

    /** 娜迦震地：命中概率召来天境娜迦的巨石（预警环 + 落石 + 震地碎石，伤害取模组倍率），占用升级槽。 */
    public static final StaticModifier<NagaQuakeModifier> nagaQuake =
            MODIFIERS.register("naga_quake", NagaQuakeModifier::new);

    /** 晶笼：受击时在攻击者脚下升起水晶石傀儡的晶笼（破土/持续伤害 + 缓慢 II），装在护甲上，占用升级槽。 */
    public static final StaticModifier<GolemCageModifier> golemCage =
            MODIFIERS.register("golem_cage", GolemCageModifier::new);

    /** 晶刺冲撞：高速踏地时沿身后踏出一串魔像晶刺（破土伤害 + 碎晶特效），装在护甲上，占用升级槽。 */
    public static final StaticModifier<GolemSpikeChargeModifier> golemSpikeCharge =
            MODIFIERS.register("golem_spike_charge", GolemSpikeChargeModifier::new);

    /** 灯灵天剑：右键对周围 12 格内每个敌人各召来一柄灯壶之灵的巨剑（原模组天剑特效 + 倍率伤害），占用能力槽。 */
    public static final StaticModifier<SpiritSwordfallModifier> spiritSwordfall =
            MODIFIERS.register("spirit_swordfall", SpiritSwordfallModifier::new);

    private AetherBedrockModifiers() {
    }
}
