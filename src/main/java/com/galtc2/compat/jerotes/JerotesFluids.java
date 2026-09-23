package com.galtc2.compat.jerotes;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtc2Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.tconstruct.fluids.block.BurningLiquidBlock;

/**
 * J村 联动材料的熔融流体。
 * <p>
 * 与 {@link JerotesModifiers} 同一套做法：**只有安装了 J村 才注册**。
 * 未安装时这 4 个流体（连同流体方块、桶物品）都不存在，不会留下"没有对应材料的孤儿流体"。
 * 注册入口 {@link #register(IEventBus)} 由 {@link JerotesCompat#init} 在确认 J村 存在后调用，
 * 因此本类里不能出现 J村 的类型引用。
 * <p>
 * 流体存在与否不影响数据生成的纹理：{@code galtcFluidGen} 里有同样的判断，
 * 未装 J村 时不会产出它们的纹理配置，这 4 个
 * {@code src/generated/resources/assets/galtc2/mantle/fluid_texture/molten_*.json}
 * 是随代码一起提交的固定文件（格式与生成器输出一致，装了 J村 再跑 runData 会写出同样的内容）。
 */
public final class JerotesFluids {

    /** 独立注册器：仅 J村 存在时才会挂到 mod 事件总线上 */
    private static final FluidDeferredRegister FLUIDS = new FluidDeferredRegister(Galtc2.MODID);

    /** 4 个联动流体的属性一致（与改版前 galtc2Fluid 里的写法保持完全一致） */
    private static final int TEMPERATURE = 1000;
    private static final int VISCOSITY = 1000;
    private static final int DENSITY = 1200;

    /** 沙耶（深绿） */
    public static final FlowingFluidObject<ForgeFlowingFluid> moltenSaya =
            create("molten_saya", MapColor.COLOR_GREEN);
    /** 勺景（近黑紫） */
    public static final FlowingFluidObject<ForgeFlowingFluid> moltenShoujing =
            create("molten_shoujing", MapColor.COLOR_PURPLE);
    /** 腐姬（血红） */
    public static final FlowingFluidObject<ForgeFlowingFluid> moltenEuthanasia =
            create("molten_euthanasia", MapColor.COLOR_RED);
    /** 清枫（灰） */
    public static final FlowingFluidObject<ForgeFlowingFluid> moltenQingfeng =
            create("molten_qingfeng", MapColor.COLOR_GRAY);

    /** 由 {@link JerotesCompat#init} 调用（仅 J村 存在时） */
    static void register(IEventBus modEventBus) {
        FLUIDS.register(modEventBus);
    }

    private static FlowingFluidObject<ForgeFlowingFluid> create(String name, MapColor mapColor) {
        return FLUIDS.register(name)
                .type(galtc2Fluid.createFluidType(TEMPERATURE, 0, VISCOSITY, DENSITY))
                .block(supplier -> new BurningLiquidBlock(supplier,
                        FluidDeferredRegister.createProperties(mapColor, 0), 10, 2.0f))
                .bucket()
                .flowing();
    }

    private JerotesFluids() {
    }
}
