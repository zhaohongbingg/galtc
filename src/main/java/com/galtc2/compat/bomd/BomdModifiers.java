package com.galtc2.compat.bomd;

import com.galtc2.Galtc2;
import com.galtc2.compat.bomd.modifiers.AncientAnimaModifier;
import com.galtc2.compat.bomd.modifiers.BlazingEyeModifier;
import com.galtc2.compat.bomd.modifiers.ObsidianHeartModifier;
import com.galtc2.compat.bomd.modifiers.SoulHeartModifier;
import com.galtc2.compat.bomd.modifiers.VoidThornModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

/**
 * 祸乱鬼魅(Bosses of Mass Destruction) 联动强化/特性的注册位，
 * 与 J村 的 {@code JerotesModifiers}、迎战 的 {@code MeetYourFightModifiers} 同一个写法。
 * <p>
 * 本类只在装了 BOMD 时由 {@link BomdCompat#init} 加载并注册；
 * 它下面的强化类会引用 BOMD 的实体/投射物/方块类型，所以<b>绝不能</b>被别处提前触碰
 * （否则未装 BOMD 时会 NoClassDefFoundError）。对应材料 JSON 也带
 * {@code forge:mod_loaded}(bosses_of_mass_destruction) 条件，两边保持一致。
 */
public final class BomdModifiers {

    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Galtc2.MODID);

    /** 魂人：材料特性（对亡灵与暗夜巫妖增伤 25%，受其伤害减少 25%）。 */
    public static final StaticModifier<SoulHeartModifier> soulHeart =
            MODIFIERS.register("soul_heart", SoulHeartModifier::new);

    /** 星空下的记忆：右键随机射出坠星或霜星（暗夜巫妖的两种弹幕）。 */
    public static final StaticModifier<AncientAnimaModifier> ancientAnima =
            MODIFIERS.register("ancient_anima", AncientAnimaModifier::new);

    /** 永恒少女：命中后在目标脚下生成 BOMD 的灌木禁壁。 */
    public static final StaticModifier<VoidThornModifier> voidThorn =
            MODIFIERS.register("void_thorn", VoidThornModifier::new);

    /** 宝石少女：命中后在目标脚下随机生成黑曜石地刺。 */
    public static final StaticModifier<ObsidianHeartModifier> obsidianHeart =
            MODIFIERS.register("obsidian_heart", ObsidianHeartModifier::new);

    /** 祐天寺美汐：按住右键射出激光，持续结算伤害。 */
    public static final StaticModifier<BlazingEyeModifier> blazingEye =
            MODIFIERS.register("blazing_eye", BlazingEyeModifier::new);

    private BomdModifiers() {
    }
}
