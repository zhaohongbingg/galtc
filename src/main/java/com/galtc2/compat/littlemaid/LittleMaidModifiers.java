package com.galtc2.compat.littlemaid;

import com.galtc2.Galtc2;
import com.galtc2.compat.littlemaid.modifiers.LoveGrowsModifier;
import com.galtc2.compat.littlemaid.modifiers.OneHeartModifier;
import com.galtc2.compat.littlemaid.modifiers.TrueLoveModifier;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

/**
 * 车万女仆 联动特性（材料词条）的注册位，
 * 与 遗境守望 的 {@code RelicWardModifiers}、迎战 的 {@code MeetYourFightModifiers} 同一个写法。
 * <p>
 * 本类只在装了车万女仆 时由 {@link LittleMaidCompat#init} 加载并注册；
 * 它下面的特性类会引用车万女仆 的 {@code EntityMaid} / 好感度类，所以<b>绝不能</b>被别处提前触碰。
 * <p>
 * 三个词条都挂在材料 traits 上（{@code data/galtc2/tinkering/materials/traits/<材料>.json}），
 * 也就是说：材料面板照常写在 {@code stats/<材料>.json}，词条只是额外效果。
 */
public final class LittleMaidModifiers {

    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Galtc2.MODID);

    /* ---------------------------------------------------------------- 词条 ID */
    // 供「查玩家/女仆身上有没有这个词条」时使用（ToolStack#getModifiers().getLevel(id)）

    public static final ModifierId LOVE_GROWS_ID = new ModifierId(Galtc2.loc("love_grows"));
    public static final ModifierId TRUE_LOVE_ID = new ModifierId(Galtc2.loc("true_love"));
    public static final ModifierId ONE_HEART_ID = new ModifierId(Galtc2.loc("one_heart"));

    /* ---------------------------------------------------------------- 词条 */

    /** 日久生情：按酒狐的好感度，为工具额外补 1/2/2/3/3/4 点基础面板（可叠加）。 */
    public static final StaticModifier<LoveGrowsModifier> loveGrows =
            MODIFIERS.register("love_grows", LoveGrowsModifier::new);

    /** 挚爱：箭矢命中后，目标 5 秒内优先索敌「除了玩家与酒狐以外」的生物，对 boss 不生效。 */
    public static final StaticModifier<TrueLoveModifier> trueLove =
            MODIFIERS.register("true_love", TrueLoveModifier::new);

    /** 同心：玩家穿 → 酒狐 80% 伤害转给玩家；酒狐穿 → 抗性提升 II + 生命恢复 III。 */
    public static final StaticModifier<OneHeartModifier> oneHeart =
            MODIFIERS.register("one_heart", OneHeartModifier::new);

    private LittleMaidModifiers() {
    }
}
