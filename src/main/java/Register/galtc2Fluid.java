package Register;

import com.galtc2.Galtc2;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.fluids.block.BurningLiquidBlock;

import javax.swing.plaf.PanelUI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;


import static slimeknights.tconstruct.fluids.block.BurningLiquidBlock.createBurning;
public class galtc2Fluid {
    public static final FluidDeferredRegister FLUIDS = new FluidDeferredRegister(Galtc2.MODID);
    public static FluidType.Properties createFluidType(int temperature, int lightLevel, int viscosity, int density) {
        return FluidType.Properties.create().temperature(temperature) // 设置流体的温度
                .lightLevel(lightLevel)    // 设置流体的亮度
                .viscosity(viscosity)      // 设置流体的粘度
                .density(density)         // 设置流体的密度
                .sound(SoundActions.BUCKET_FILL,
                        SoundEvents.BUCKET_FILL_LAVA) //Does there real difference between FILL WATER and LAVA? I dont know
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA);
    }
    private static FlowingFluidObject<ForgeFlowingFluid> registerFluid(FluidDeferredRegister register, String name, int temp, int viscosity, int density, int lightLevel, Function<Supplier<? extends FlowingFluid>, LiquidBlock> blockFunction) {
        return register.register(name).type(createFluidType(temp, lightLevel, viscosity, density)).block(blockFunction).bucket().flowing();
    }
    public static final  FlowingFluidObject<ForgeFlowingFluid> molten_sprb=
            registerFluid(FLUIDS,"molten_sprb",1000,1000,100,10,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_BLACK, 0), 10, 0) {});

    public static final FlowingFluidObject<ForgeFlowingFluid> molten_zeroone =
            registerFluid(FLUIDS, "molten_zeroone", 800, 1500, 1800, 8,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.METAL, 8), 12, 3.0f));
    // 低温精密流体 - kagome (网格结构)
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_kagome =
            registerFluid(FLUIDS, "molten_kagome", 600, 1200, 2000, 6,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.DIAMOND, 6), 8, 2.5f));
    // - aosora
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_aosora =
            registerFluid(FLUIDS, "molten_aosora", 900, 800, 1400, 10,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_LIGHT_BLUE, 10), 10, 3.5f));
    //  - kagari
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_kagari =
            registerFluid(FLUIDS, "molten_kagari", 1800, 3000, 2800, 15,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.FIRE, 15), 18, 6.0f));
    //  - arti
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_arti =
            registerFluid(FLUIDS, "molten_arti", 700, 1000, 1600, 7,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_PURPLE, 7), 9, 2.0f));
    //  - apeiria (无限)
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_apeiria =
            registerFluid(FLUIDS, "molten_apeiria", 2200, 5000, 3500, 15,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_MAGENTA, 15), 20, 8.0f));
    //  - einstein
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_einstein =
            registerFluid(FLUIDS, "molten_einstein", 1200, 1800, 2200, 11,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.QUARTZ, 11), 13, 4.5f));
    //  - narcissus (水仙)
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_narcissus =
            registerFluid(FLUIDS, "molten_narcissus", 500, 600, 1200, 5,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 5), 6, 1.5f));





public  static  void  register(){

}
}
