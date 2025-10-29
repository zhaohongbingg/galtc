package Register;


import com.galtc2.Galtc2;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static com.galtc2.Galtc2.CREATIVE_MODE_TABS;

public class galtctag {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB_DEFERRED_REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Galtc2.MODID);
    public static final RegistryObject<CreativeModeTab> galtctab =
            CREATIVE_MODE_TAB_DEFERRED_REGISTER.register("galtc2",()-> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(galtcItem.SPRB.get()))
                    .title(Component.translatable("itemGroup.galtctab"))
                    .displayItems((pParameters,pOutput)->{
                        pOutput.accept(galtcItem.Test.get());
                        pOutput.accept(galtcItem.SPRB.get());
                    })
                    .build());



public static void register(IEventBus eventBus){
    CREATIVE_MODE_TAB_DEFERRED_REGISTER.register(eventBus);
}


}
