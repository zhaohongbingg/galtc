package com.galtc2.compat.meetyourfight;

import com.galtc2.Galtc2;
import com.galtc2.compat.meetyourfight.modifiers.GutsModifier;
import com.galtc2.compat.meetyourfight.modifiers.HolyWarriorModifier;
import com.galtc2.compat.meetyourfight.modifiers.KinBondModifier;
import com.galtc2.compat.meetyourfight.modifiers.KlaroroModifier;
import com.galtc2.compat.meetyourfight.modifiers.MoonCleaveModifier;
import com.galtc2.compat.meetyourfight.modifiers.MossySpikeModifier;
import com.galtc2.compat.meetyourfight.modifiers.RoseWraithModifier;
import com.galtc2.compat.meetyourfight.modifiers.SoulEaterModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

/**
 * 迎战(Meet Your Fight) 联动强化/特性的注册位，和 J村 的 {@code JerotesModifiers} 一个写法。
 * <p>
 * 本类只在装了迎战 时由 {@link MeetYourFightCompat#init} 加载并注册，
 * 对应材料 JSON 也带 {@code forge:mod_loaded}(meetyourfight) 条件，两边一致。
 */
public final class MeetYourFightModifiers {

    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Galtc2.MODID);

    /** 魂喰：幽魂质 材料特性（+2 方块触及距离，对发光生物加伤）。 */
    public static final StaticModifier<SoulEaterModifier> soulEater =
            MODIFIERS.register("soul_eater", SoulEaterModifier::new);

    /** 斩月双刀：命运之眷 材料的攻击侧特性（20% 概率双倍伤害）。 */
    public static final StaticModifier<MoonCleaveModifier> moonCleave =
            MODIFIERS.register("moon_cleave", MoonCleaveModifier::new);

    /** 圣战士：命运之眷 材料的防御侧特性（每件护甲 1/6 概率免疫本次伤害）。 */
    public static final StaticModifier<HolyWarriorModifier> holyWarrior =
            MODIFIERS.register("holy_warrior", HolyWarriorModifier::new);

    /** 亲人的羁绊：命运之眷 材料特性（攻击或受击时 20% 概率获得正面 buff）。 */
    public static final StaticModifier<KinBondModifier> kinBond =
            MODIFIERS.register("kin_bond", KinBondModifier::new);

    /** 根性：藓牙 材料的防御侧特性（超过最大生命一半的伤害减半）。 */
    public static final StaticModifier<GutsModifier> guts =
            MODIFIERS.register("guts", GutsModifier::new);

    /** 藓牙牙刺：藓牙 材料的攻击侧特性（+2 穿甲伤害）。 */
    public static final StaticModifier<MossySpikeModifier> mossySpike =
            MODIFIERS.register("mossy_spike", MossySpikeModifier::new);

    /** 舞亚：暮光紫罗兰 材料的防御侧特性（受击召唤蔷薇怨灵反击）。 */
    public static final StaticModifier<RoseWraithModifier> roseWraith =
            MODIFIERS.register("rose_wraith", RoseWraithModifier::new);

    /** 克拉罗罗：击杀 boss 永久 +5% 全属性（迎战 4 件掉落物 + 1 能力槽，工匠站安装）。 */
    public static final StaticModifier<KlaroroModifier> klaroro =
            MODIFIERS.register("klaroro", KlaroroModifier::new);

    private MeetYourFightModifiers() {
    }
}
