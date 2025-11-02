package Register;

import com.galtc2.Galtc2;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class galtcItem {
    public static final DeferredRegister<Item> ITEMS=
            DeferredRegister.create(ForgeRegistries.ITEMS, Galtc2.MODID);
//夏日锭
    public  static  final   RegistryObject<Item> sprb =
            ITEMS.register("sprb",()->new Item(new Item.Properties()));

    //零一锭
    public static final RegistryObject<Item> zeroone =
            ITEMS.register("zeroone",()->new Item(new Item.Properties()));
    public static  final RegistryObject<Item> kagome =
            ITEMS.register("kagome",()->new Item(new Item.Properties()));//笼女
    public static  final RegistryObject<Item>  aosora=
            ITEMS.register("aosora",()->new Item(new Item.Properties()));//苍彼
    public  static  final RegistryObject<Item> kagari=
            ITEMS.register("kagari",()->new Item(new Item.Properties()));//篝
    public static final RegistryObject<Item> atri=
            ITEMS.register("atri",()->new Item(new Item.Properties()));//arti
    public static final RegistryObject<Item> apeiria=
            ITEMS.register("apeiria",()->new Item(new Item.Properties()));//艾佩莉雅
    public  static  final  RegistryObject<Item> einstein=
            ITEMS.register("einstein",()->new Item(new Item.Properties()));// 爱因斯坦挟爱之上
    public  static  final  RegistryObject<Item> narcissus=
            ITEMS.register("narcissus",()->new Item(new Item.Properties()));//水仙
    public  static  final  RegistryObject<Item> ca =
            ITEMS.register("ca",()->new Item(new Item.Properties())); //CA(水葬）



    public static final List<RegistryObject<Item>> ALL_ITEMS = List.of(
            sprb,
            zeroone,
            kagome,
            aosora,
            kagari,
            atri,
            apeiria,
            einstein,
            narcissus,
            ca
    );







    public static void register(){
    }
}
