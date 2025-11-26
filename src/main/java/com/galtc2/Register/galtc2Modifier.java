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






   public static void register(){}

}
