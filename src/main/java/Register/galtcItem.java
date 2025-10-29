package Register;

import com.galtc2.Galtc2;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class galtcItem {
    public static final DeferredRegister<Item> ITEMS=
            DeferredRegister.create(ForgeRegistries.ITEMS, Galtc2.MODID);
    public static final RegistryObject<Item> Test =
            ITEMS.register("test",()->new Item( new Item.Properties()));
    public  static  final   RegistryObject<Item> SPRB =
            ITEMS.register("sprb",()->new Item(new Item.Properties()));
    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
