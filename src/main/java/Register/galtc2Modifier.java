package Register;

import com.galtc2.Galtc2;
import modifiers.*;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

import java.util.function.Supplier;

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




   public static void register(){}

}
