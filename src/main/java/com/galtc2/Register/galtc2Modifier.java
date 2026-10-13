package com.galtc2.Register;

import com.galtc2.Galtc2;
import com.galtc2.modifiers.*;

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



   public static void register(){}

}
