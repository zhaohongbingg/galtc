package com.galtc2.jvt.Register;

import com.galtc2.jvt.Jvt;
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

/** 熔融流体注册。新增金属时照下面的模板再注册一个 FlowingFluidObject 即可。 */
public class JvtFluid {
    public static final FluidDeferredRegister FLUIDS = new FluidDeferredRegister(Jvt.MODID);

    public static FluidType.Properties createFluidType(int temperature, int lightLevel, int viscosity, int density) {
        return FluidType.Properties.create()
                .temperature(temperature)
                .lightLevel(lightLevel)
                .viscosity(viscosity)
                .density(density)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                .canConvertToSource(false)
                .canDrown(true)
                .canExtinguish(false)
                .canHydrate(false)
                .canPushEntity(true)
                .canSwim(true)
                .supportsBoating(false);
    }

    private static FlowingFluidObject<ForgeFlowingFluid> registerFluid(FluidDeferredRegister register, String name, int temp, int viscosity, int density, int lightLevel, Function<Supplier<? extends FlowingFluid>, LiquidBlock> blockFunction) {
        return register.register(name).type(createFluidType(temp, lightLevel, viscosity, density)).block(blockFunction).bucket().flowing();
    }

    /** 示例材料: 村民金属(铜+铁冶炼的合金，J村产物) */
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_villager_metal =
            registerFluid(FLUIDS, "molten_villager_metal", 1000, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.METAL, 0), 10, 2.0f));

    /** 鲙酷金属 */
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_qoaiku_metal =
            registerFluid(FLUIDS, "molten_qoaiku_metal", 1300, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.METAL, 0), 10, 2.0f));

    /** 蛇龙合金 */
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_serpon_alloy =
            registerFluid(FLUIDS, "molten_serpon_alloy", 1350, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.METAL, 0), 10, 2.0f));

    /** 莫厄金属 */
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_meror =
            registerFluid(FLUIDS, "molten_meror", 1400, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.METAL, 0), 10, 2.0f));

    /** 精炼莫厄金属 */
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_refine_meror =
            registerFluid(FLUIDS, "molten_refine_meror", 1500, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.METAL, 0), 10, 2.0f));

    /** 古二轮金属(铁+钴合金) */
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_ancient_second_rounder =
            registerFluid(FLUIDS, "molten_ancient_second_rounder", 1450, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.METAL, 0), 10, 2.0f));

    /** 火焰使徒金属 */
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_fire_apostle =
            registerFluid(FLUIDS, "molten_fire_apostle", 1550, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.METAL, 0), 10, 2.0f));

    /** 镇山金属 */
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_mountain_realm =
            registerFluid(FLUIDS, "molten_mountain_realm", 1500, 1200, 1500, 0,
                    supplier -> new BurningLiquidBlock(supplier, FluidDeferredRegister.createProperties(MapColor.METAL, 0), 10, 2.0f));

    public static void register() {
    }
}
