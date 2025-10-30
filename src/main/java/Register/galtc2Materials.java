package Register;

import com.galtc2.Galtc2;
import net.minecraft.client.resources.model.Material;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;

import java.util.HashMap;
import java.util.Map;

import static Register.galtcItem.ALL_ITEMS;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class galtc2Materials {
    public static MaterialId createMaterial(String name) {
        return new MaterialId(ResourceLocation.parse(Galtc2.MODID + ":" + name));   }
    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();
        if (event.includeClient()) {
        }
        if (event.includeServer()) {
        }
    }
    public static final MaterialId sprb = createMaterial("sprb");
    public static final MaterialId zeroone = createMaterial("zeroone");
    public static final MaterialId kagome = createMaterial("kagome");
    public static final MaterialId aosora = createMaterial("aosora");
    public static final MaterialId kagari = createMaterial("kagari");
    public static final MaterialId arti = createMaterial("arti");
    public static final MaterialId apeiria = createMaterial("apeiria");
    public static final MaterialId einstein = createMaterial("einstein");
    public static final MaterialId narcissus = createMaterial("narcissus");


    public static void register() {}




}

