package com.galtc2.Register;

import com.galtc2.Galtc2;
import com.galtc2.modifiers.*;
import com.galtc2.modifiers.armor.*;
import com.galtc2.jvt.modifiers.MerorRangedModifier;

import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;




public class galtc2Modifier {


    public static ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Galtc2.MODID);
    public static final StaticModifier<Rude> rude = MODIFIERS.register("rude", Rude::new);
    public static final StaticModifier<power>  Power = MODIFIERS.register("power",power::new);

    public static  final  StaticModifier<Sprb> sprb = MODIFIERS.register("sprb",Sprb::new);

    public static  final  StaticModifier<Celestial> Celestial =MODIFIERS.register("celestial",Celestial::new);

    public static  final  StaticModifier<zhuyin> Zhuyin =MODIFIERS.register("zhuyin",zhuyin::new);

    public  static  final  StaticModifier<eightstrike> Eightstrike = MODIFIERS.register("eightstrike",eightstrike::new);

    public  static  final StaticModifier<Kagome> kagome= MODIFIERS.register("kagome",Kagome::new);
    public static  final  StaticModifier<broke> Broke = MODIFIERS.register("broke",broke::new);

    public  static  final  StaticModifier<leeched> leeched =MODIFIERS.register("leeched",leeched::new);

    public static  final StaticModifier<narcissus> narcissus = MODIFIERS.register("narcissus",narcissus::new);

    public static final StaticModifier<Valuation> valuation = MODIFIERS.register("valuation",Valuation::new);

    public static  final StaticModifier<Kaleidoscope> kaleidoscope = MODIFIERS.register("kaleidoscope",Kaleidoscope::new);

    public static  final StaticModifier<SteinsGate> steinsgate = MODIFIERS.register("steinsgate",SteinsGate::new);

    public  static  final StaticModifier<azarashi> azarashi = MODIFIERS.register("azarashi",azarashi::new);

    public static final StaticModifier<NightTimeSheep> nighttimesheep = MODIFIERS.register("nighttimesheep",NightTimeSheep::new);

    public static final StaticModifier<oumunnbyou> oumunnbyou = MODIFIERS.register("oumunnbyou",oumunnbyou::new);

    public static final StaticModifier<KisaragiKurenai> kisaragiKurenai = MODIFIERS.register("kisaragikurenai",KisaragiKurenai::new);

    public  static  final  StaticModifier<Tagetes> tagetes = MODIFIERS.register("tagetes",Tagetes::new);

    public static  final  StaticModifier<Rosmarinus> rosmarinus = MODIFIERS.register("rosmarinus",Rosmarinus::new);

    public  static  final StaticModifier<lavare> lavare = MODIFIERS.register("lavare",lavare::new);

    public static final StaticModifier<Muramasa> muramasa = MODIFIERS.register("muramasa",Muramasa::new);

    public  static final StaticModifier<AIDefense> aiDefense = MODIFIERS.register("aid",AIDefense::new);

    public static final StaticModifier<AIArmor> aiArmor = MODIFIERS.register("aia",AIArmor::new);

    public static final StaticModifier<Shochou> shochou = MODIFIERS.register("shochou",Shochou::new);

    public static final StaticModifier<SpoiledByRichGirl> spoiled_by_rich_girl = MODIFIERS.register("spoiled_by_rich_girl",SpoiledByRichGirl::new);
    public static final StaticModifier<HealedBySister> healed_by_sister = MODIFIERS.register("healed_by_sister",HealedBySister::new);

    public static final StaticModifier<MeatbunModifier> meatbun = MODIFIERS.register("meatbun", MeatbunModifier::new);
    public static final StaticModifier<VtbModifier> vtb = MODIFIERS.register("vtb", VtbModifier::new);
    public static final StaticModifier<QienModifier> qien = MODIFIERS.register("qien", QienModifier::new);
    public static final StaticModifier<YidianModifier> yidian = MODIFIERS.register("yidian", YidianModifier::new);
    public static final StaticModifier<RewriteModifier> rewrite = MODIFIERS.register("rewrite", RewriteModifier::new);
    /** 改写（护甲侧）：被打时把攻击者的护甲/生命偷过来。 */
    public static final StaticModifier<RewriteArmorModifier> rewrite_armor = MODIFIERS.register("rewrite_armor", RewriteArmorModifier::new);
    /** 樱花印记（护甲侧）：被打时把樱花印记叠给攻击者。 */
    public static final StaticModifier<OumnnbyouArmorModifier> oumnnbyou_armor = MODIFIERS.register("oumnnbyou_armor", OumnnbyouArmorModifier::new);
    public static final StaticModifier<HeartWallModifier> heartwall = MODIFIERS.register("heartwall", HeartWallModifier::new);
    public static final StaticModifier<ZouLeZouLeModifier> zoulezoule = MODIFIERS.register("zoulezoule", ZouLeZouLeModifier::new);
    // 卡缪斯 / 莫尔加那：攻防各自独立成条（原 camus / morgana 保留注册，供旧存档里的工具继续生效）
    public static final StaticModifier<CamusModifier> camus = MODIFIERS.register("camus", CamusModifier::new);
    public static final StaticModifier<CamusMeleeModifier> camus_melee = MODIFIERS.register("camus_melee", CamusMeleeModifier::new);
    public static final StaticModifier<CamusArmorModifier> camus_armor = MODIFIERS.register("camus_armor", CamusArmorModifier::new);
    public static final StaticModifier<WonderfulModifier> wonderful = MODIFIERS.register("wonderful", WonderfulModifier::new);
    public static final StaticModifier<StarryTravelModifier> starry_travel = MODIFIERS.register("starry_travel", StarryTravelModifier::new);
    public static final StaticModifier<StarryTravelArmorModifier> starry_travel_armor = MODIFIERS.register("starry_travel_armor", StarryTravelArmorModifier::new);
    public static final StaticModifier<SuperLuckyModifier> super_lucky = MODIFIERS.register("super_lucky", SuperLuckyModifier::new);

    /** 你怎么这么熟练：每使用 100 次 +1% 全属性（能力槽）。 */
    public static final StaticModifier<SkilledModifier> skilled = MODIFIERS.register("skilled", SkilledModifier::new);


    public static final StaticModifier<CialloModifier> ciallo = MODIFIERS.register("ciallo", CialloModifier::new);

    public static final StaticModifier<KisakiModifier> kisaki = MODIFIERS.register("kisaki", KisakiModifier::new);

    public static final StaticModifier<KisakiLikesModifier> kisaki_likes = MODIFIERS.register("kisaki_likes", KisakiLikesModifier::new);

    public static final StaticModifier<KisakiHatesModifier> kisaki_hates = MODIFIERS.register("kisaki_hates", KisakiHatesModifier::new);

    // 沙耶 / 勺景：攻防已各自独立成条（原 saya / shoujing 保留注册，供旧存档里的工具继续生效）
    public static final StaticModifier<SayaModifier> saya = MODIFIERS.register("saya", SayaModifier::new);
    public static final StaticModifier<SayaMeleeModifier> saya_melee = MODIFIERS.register("saya_melee", SayaMeleeModifier::new);
    public static final StaticModifier<SayaArmorModifier> saya_armor = MODIFIERS.register("saya_armor", SayaArmorModifier::new);
    public static final StaticModifier<ShoujingModifier> shoujing = MODIFIERS.register("shoujing", ShoujingModifier::new);
    public static final StaticModifier<ShoujingMeleeModifier> shoujing_melee = MODIFIERS.register("shoujing_melee", ShoujingMeleeModifier::new);
    public static final StaticModifier<ShoujingArmorModifier> shoujing_armor = MODIFIERS.register("shoujing_armor", ShoujingArmorModifier::new);
    public static final StaticModifier<EuthanasiaModifier> euthanasia = MODIFIERS.register("euthanasia", EuthanasiaModifier::new);
    public static final StaticModifier<QingfengModifier> qingfeng = MODIFIERS.register("qingfeng", QingfengModifier::new);
    public static final StaticModifier<MorganaModifier> morgana = MODIFIERS.register("morgana", MorganaModifier::new);
    public static final StaticModifier<MorganaMeleeModifier> morgana_melee = MODIFIERS.register("morgana_melee", MorganaMeleeModifier::new);
    public static final StaticModifier<MorganaArmorModifier> morgana_armor = MODIFIERS.register("morgana_armor", MorganaArmorModifier::new);

    /** 天津罪：手持/穿戴时展开禁飞领域（材料特性）。 */
    public static final StaticModifier<TianjinZuiModifier> tianjin_zui = MODIFIERS.register("tianjin_zui", TianjinZuiModifier::new);

    /** 天津罪·其二：领域内生物无法生成（材料特性，逻辑见 TianjinZuiSpawnBanEvents）。 */
    public static final StaticModifier<TianjinZuiSpawnBanModifier> tianjin_zui_no_spawn =
            MODIFIERS.register("tianjin_zui_no_spawn", TianjinZuiSpawnBanModifier::new);

    // 防御特性（护甲侧）：与同名材料的工具特性镜像，只在护甲部件上生效
    public static final StaticModifier<SprbArmor> sprb_armor = MODIFIERS.register("sprb_armor", SprbArmor::new);
    public static final StaticModifier<ZeroOneArmor> zeroone_armor = MODIFIERS.register("zeroone_armor", ZeroOneArmor::new);
    public static final StaticModifier<NarcissusArmor> narcissus_armor = MODIFIERS.register("narcissus_armor", NarcissusArmor::new);
    public static final StaticModifier<CaArmor> ca_armor = MODIFIERS.register("ca_armor", CaArmor::new);
    public static final StaticModifier<AzarashiArmor> azarashi_armor = MODIFIERS.register("azarashi_armor", AzarashiArmor::new);
    public static final StaticModifier<NightTimeSheepArmor> nighttimesheep_armor =
            MODIFIERS.register("nighttimesheep_armor", NightTimeSheepArmor::new);
    public static final StaticModifier<KisaragiKurenaiArmor> kisaragikurenai_armor =
            MODIFIERS.register("kisaragikurenai_armor", KisaragiKurenaiArmor::new);
    public static final StaticModifier<BlackLilyArmor> blacklily_armor = MODIFIERS.register("blacklily_armor", BlackLilyArmor::new);
    public static final StaticModifier<ShochouArmor> shochou_armor = MODIFIERS.register("shochou_armor", ShochouArmor::new);
    public static final StaticModifier<CialloArmor> ciallo_armor = MODIFIERS.register("ciallo_armor", CialloArmor::new);

    // 阴影（取自天境基石生物重制 阴影套）：工具/远程/护甲三条龙系特性
    public static final StaticModifier<ShadayModifier> shaday = MODIFIERS.register("shaday", ShadayModifier::new);
    public static final StaticModifier<ShadayRangedModifier> shaday_ranged =
            MODIFIERS.register("shaday_ranged", ShadayRangedModifier::new);
    public static final StaticModifier<ShadayArmor> shaday_armor = MODIFIERS.register("shaday_armor", ShadayArmor::new);

    /** 苍彼：近战命中把目标向上抛起约 64 格，之后自然下坠（只认手持武器，护甲不触发）。 */
    public static final StaticModifier<AosoraLaunchModifier> aosora = MODIFIERS.register("aosora", AosoraLaunchModifier::new);

    /* ---------------------------------------------------------------- 远程（弓/弩）特性 */

    /**
     * 零八式·乱射（zeroone 远程）：一次射击 8 支箭，每支 12.5% 箭伤（8 支合计 ≈ 1 支），只消耗 1 支弹药。
     * 实现在 {@link com.galtc2.modifiers.EightStrikeRangedModifier}（PROJECTILE_LAUNCH 里补射）。
     */
    public static final StaticModifier<EightStrikeRangedModifier> eightstrike_ranged =
            MODIFIERS.register("eightstrike_ranged", EightStrikeRangedModifier::new);

    /**
     * 莫厄箭（莫厄 / 精炼莫厄 远程）：箭矢命中追加 箭矢基础伤害 × 50% × 等级（1 级 +50%、2 级 +100%），
     * 伤害类型 {@code galtc2:meror_shot}，并由 J村 事件认作莫厄伤害。
     * 实现在 {@link MerorRangedModifier}（PROJECTILE_HIT，每支箭只结算一次）。
     */
    public static final StaticModifier<MerorRangedModifier> meror_bolt =
            MODIFIERS.register("meror_bolt", MerorRangedModifier::new);

   public static void register(){}

}
