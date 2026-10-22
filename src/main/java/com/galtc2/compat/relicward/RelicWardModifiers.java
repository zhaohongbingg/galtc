package com.galtc2.compat.relicward;

import com.galtc2.Galtc2;
import com.galtc2.compat.relicward.modifiers.BellWardModifier;
import com.galtc2.compat.relicward.modifiers.EchoSlamModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

// 以下三条是早期写好的、暂时没挂到任何材料上的特性（按需求注释保留，源码仍在
// com/galtc2/compat/relicward/modifiers/ 下，要启用就把对应注册和 import 一起放开）：
//import com.galtc2.compat.relicward.modifiers.ArmorFractureModifier;
//import com.galtc2.compat.relicward.modifiers.PuppetSummonModifier;
//import com.galtc2.compat.relicward.modifiers.ResonantWaveModifier;

/**
 * 遗境守望(Relic Ward) 联动强化/特性的注册位，
 * 与 祸乱鬼魅 的 {@code BomdModifiers}、迎战 的 {@code MeetYourFightModifiers} 同一个写法。
 * <p>
 * 本类只在装了遗境守望 时由 {@link RelicWardCompat#init} 加载并注册；
 * 它下面的强化类会引用遗境守望 的实体/效果/工具类，所以<b>绝不能</b>被别处提前触碰
 * （否则未装遗境守望 时会 NoClassDefFoundError）。
 */
public final class RelicWardModifiers {

    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Galtc2.MODID);

    /** 回响震击：右键蓄力后松手，前方范围伤害 + 裂甲 + 击退（挂在 钟铸铜板 的攻击侧）。 */
    public static final StaticModifier<EchoSlamModifier> echoSlam =
            MODIFIERS.register("echo_slam", EchoSlamModifier::new);

    /** 钟甲：每级 +5% 抗击退，单次 6 点以上伤害减免 30%（挂在 钟铸铜板 的盔甲侧）。 */
    public static final StaticModifier<BellWardModifier> bellWard =
            MODIFIERS.register("bell_ward", BellWardModifier::new);

    // —— 以下三个特性目前没有被任何材料/配方用到，先不注册（源码保留，随时可启用）——
    //
    ///** 裂甲：近战/箭矢命中时给目标打上遗境守望 的「裂甲」（暂时降低护甲与护甲韧性）。 */
    //public static final StaticModifier<ArmorFractureModifier> armorFracture =
    //        MODIFIERS.register("armor_fracture", ArmorFractureModifier::new);
    //
    ///** 钟波：站地面右键释放扩散钟波（复用震律鸣钟的 ResonantWave）。 */
    //public static final StaticModifier<ResonantWaveModifier> resonantWave =
    //        MODIFIERS.register("resonant_wave", ResonantWaveModifier::new);
    //
    ///** 巡庭铜傀：右键召唤一只认自己为主的铜傀伙伴。 */
    //public static final StaticModifier<PuppetSummonModifier> courtPuppet =
    //        MODIFIERS.register("court_puppet", PuppetSummonModifier::new);

    private RelicWardModifiers() {
    }
}
