package com.galtc2.Register;

import com.galtc2.Galtc2;

import net.minecraft.sounds.SoundEvents;

import net.minecraft.world.level.block.LiquidBlock;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;

import net.minecraftforge.common.SoundActions;

import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.object.FlowingFluidObject;

import slimeknights.tconstruct.fluids.block.BurningLiquidBlock;


import java.util.function.Function;
import java.util.function.Supplier;

public class galtc2Fluid {
    public static final FluidDeferredRegister FLUIDS = new FluidDeferredRegister(Galtc2.MODID);

    public static FluidType.Properties createFluidType(int temperature, int lightLevel, int viscosity, int density) {
        return FluidType.Properties.create()
                .temperature(temperature)     // 设置流体的温度
                .lightLevel(lightLevel)       // 设置流体的亮度
                .viscosity(viscosity)         // 设置流体的粘度
                .density(density)            // 设置流体的密度
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                .canConvertToSource(false)    // 防止无限流体源
                .canDrown(true)              // 可以淹没实体
                .canExtinguish(false)        // 不能灭火
                .canHydrate(false)           // 不能浇灌作物
                .canPushEntity(true)         // 可以推动实体
                .canSwim(true)               // 可以游泳
                .supportsBoating(false);     // 不支持船只
    }

    private static FlowingFluidObject<ForgeFlowingFluid> registerFluid(FluidDeferredRegister register, String name, int temp, int viscosity, int density, int lightLevel, Function<Supplier<? extends FlowingFluid>, LiquidBlock> blockFunction) {
        return register.register(name).type(createFluidType(temp, lightLevel, viscosity, density)).block(blockFunction).bucket().flowing();
    }

    public static final  FlowingFluidObject<ForgeFlowingFluid> molten_sprb=
            registerFluid(FLUIDS,"molten_sprb",1000,1000,100,0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_BLACK, 0), 10, 0) {});

    public static final FlowingFluidObject<ForgeFlowingFluid> molten_zeroone =
            registerFluid(FLUIDS, "molten_zeroone", 800, 1500, 1800, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.METAL, 0), 12, 3.0f));
    // 低温精密流体 - kagome (网格结构)
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_kagome =
            registerFluid(FLUIDS, "molten_kagome", 600, 1200, 2000, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.DIAMOND, 0), 8, 2.5f));
    //  - aosora
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_aosora =
            registerFluid(FLUIDS, "molten_aosora", 900, 800, 1400, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_LIGHT_BLUE, 10), 10, 3.5f));
    //  - arti
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_atri =
            registerFluid(FLUIDS, "molten_atri", 700, 1000, 1600, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_PURPLE, 0), 9, 2.0f));
    //  - apeiria (无限)
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_apeiria =
            registerFluid(FLUIDS, "molten_apeiria", 2200, 5000, 3500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_MAGENTA, 0), 20, 8.0f));
    //  - narcissus (水仙)
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_narcissus =
            registerFluid(FLUIDS, "molten_narcissus", 500, 600, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 6, 1.5f));
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_ca =
            registerFluid(FLUIDS, "molten_ca", 500, 600, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 6, 1.5f));

    public static final FlowingFluidObject<ForgeFlowingFluid> molten_steinsgate =
                registerFluid(FLUIDS, "molten_steinsgate", 500, 600, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 6, 1.5f));
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_azarashi =
                registerFluid(FLUIDS, "molten_azarashi", 500, 600, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 6, 1.5f));
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_nighttimesheep =
            registerFluid(FLUIDS, "molten_nighttimesheep", 500, 600, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 6, 1.5f));
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_oumunnbyou =
            registerFluid(FLUIDS, "molten_oumunnbyou", 500, 600, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 6, 1.5f));
    public  static  final  FlowingFluidObject<ForgeFlowingFluid> molten_kisaragikurenai =
            registerFluid(FLUIDS, "molten_kisaragikurenai", 500, 600, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 6, 1.5f));
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_blacklily =
            registerFluid(FLUIDS, "molten_blacklily", 500, 600, 1200, 0,

                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 6, 1.5f));
public static final FlowingFluidObject<ForgeFlowingFluid> molten_muramasa =
            registerFluid(FLUIDS, "molten_muramasa", 500, 600, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 6, 1.5f));

    public static final FlowingFluidObject<ForgeFlowingFluid> molten_shochou =
            registerFluid(FLUIDS, "molten_shochou", 900, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_BROWN, 0), 10, 2.0f));

    public static final FlowingFluidObject<ForgeFlowingFluid> molten_izumi_hiyori =
            registerFluid(FLUIDS, "molten_izumi_hiyori", 800, 1000, 1300, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 8, 1.5f));

    public static final FlowingFluidObject<ForgeFlowingFluid> molten_tsukiyashiro_kisaki =
            registerFluid(FLUIDS, "molten_tsukiyashiro_kisaki", 900, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 10, 2.0f));

    public static final FlowingFluidObject<ForgeFlowingFluid> molten_nishiki_asumi =
            registerFluid(FLUIDS, "molten_nishiki_asumi", 900, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_ORANGE, 0), 10, 2.0f));

    public static final FlowingFluidObject<ForgeFlowingFluid> molten_qienidian =
            registerFluid(FLUIDS, "molten_qienidian", 800, 1000, 1300, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_LIGHT_BLUE, 0), 8, 1.5f));

    public static final FlowingFluidObject<ForgeFlowingFluid> molten_rewrite =
            registerFluid(FLUIDS, "molten_rewrite", 900, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_ORANGE, 0), 10, 2.0f));

    public static final FlowingFluidObject<ForgeFlowingFluid> molten_badwoman =
            registerFluid(FLUIDS, "molten_badwoman", 800, 1000, 1300, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_PINK, 0), 8, 1.5f));

    public static final FlowingFluidObject<ForgeFlowingFluid> molten_galaxy_railroad =
            registerFluid(FLUIDS, "molten_galaxy_railroad", 1000, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_BLUE, 0), 10, 2.0f));

    // Ciallo
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_ciallo =
            registerFluid(FLUIDS, "molten_ciallo", 1000, 1000, 100, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_GREEN, 0), 10, 0) {});

    // 矿石碎片流体
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_juqing =
            registerFluid(FLUIDS, "molten_juqing", 600, 600, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_YELLOW, 0), 6, 1.0f));
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_seqi =
            registerFluid(FLUIDS, "molten_seqi", 500, 600, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_PINK, 0), 6, 1.0f));
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_feimeng =
            registerFluid(FLUIDS, "molten_feimeng", 400, 600, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_LIGHT_BLUE, 0), 6, 1.0f));
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_guro =
            registerFluid(FLUIDS, "molten_guro", 800, 800, 1300, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_RED, 0), 8, 1.5f));

    // 莫尔加那（暗紫）
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_morgana =
            registerFluid(FLUIDS, "molten_morgana", 1000, 1000, 1200, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.COLOR_PURPLE, 0), 10, 2.0f));

    // 沙耶 / 勺景 / 腐姬 / 清枫 这 4 个 J村 联动材料的流体不在这里注册：
    // 未安装 J村 时它们不应该存在，因此挪到了 J村 存在才挂到事件总线上的
    // com.galtc2.compat.jerotes.JerotesFluids 里（由 JerotesCompat.init 触发注册）。




public  static  void  register(){

}
}
