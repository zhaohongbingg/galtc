package com.galtc2.jvt;

import com.galtc2.Galtc2;
import com.galtc2.compat.jerotes.JerotesCompat;
import com.galtc2.jvt.Register.JvtCreativeTabs;
import com.galtc2.jvt.Register.JvtFluid;
import com.galtc2.jvt.Register.JvtMetal;
import com.galtc2.jvt.Register.JvtModifier;
import com.galtc2.jvt.event.NobleFactionEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * JV Tinkers 部分的装配入口（原独立模组 {@code jvt}，现已并入 galtc2）。
 * <p>
 * 并入后命名空间统一取 {@link Galtc2#MODID}，所以原来的 {@code jvt:xxx} 全部变成
 * {@code galtc2:xxx}（材料、物品、熔融流体、强化、配方、贴图路径都跟着走）。
 * <p>
 * 它不再带 {@code @Mod} 注解 —— 一个 jar 只能有一个 mod，注册改由 {@link Galtc2}
 * 的构造函数调用 {@link #init(IEventBus)} 完成；datagen（流体贴图）也统一挂在
 * {@code Galtc2.ModEvents#gatherData} 上。
 * <p>
 * <b>J村 软联动</b>：JV 这一套（联动合金、物品、熔融流体、强化、创造栏、阵营事件）全部
 * 建立在 J村(Jerotes Village) 之上，所以未安装 J村 时<b>整体不注册</b> —— 游戏里不会出现
 * 任何 JV 内容；配合 {@code data/galtc2/tinkering/materials/**} 里的
 * {@code mod_loaded} 条件，匠魂那边也不会出现这些联动材料。
 * 引用 J村 类型的类只能从这里按需加载，禁止 {@code @Mod.EventBusSubscriber} 自动注册。
 */
public final class Jvt {

    /** 命名空间：并入 galtc2 后统一用 galtc2 的 modid */
    public static final String MODID = Galtc2.MODID;

    private static final Logger LOGGER = LogUtils.getLogger();

    private Jvt() {
    }

    /** 由 {@link Galtc2} 的 mod 事件总线调用，注册 JV 部分的所有内容（未装 J村 时整体跳过）。 */
    public static void init(IEventBus modEventBus) {
        if (!JerotesCompat.isJerotesLoaded()) {
            LOGGER.info("[galtc2-jvt] 未检测到 J村({})，跳过 JV Tinkers 全部内容：联动合金 / 物品 / 流体 / 强化 / 创造栏 / 阵营事件。",
                    JerotesCompat.JEROTES_MOD_ID);
            return;
        }
        JvtFluid.FLUIDS.register(modEventBus);
        // 3 种自有金属（金属块 / 锭 / 粒）：BlockDeferredRegister 自带 block 与 item 两张注册表
        JvtMetal.BLOCKS.register(modEventBus);
        JvtModifier.MODIFIERS.register(modEventBus);
        JvtCreativeTabs.CREATIVE_TABS.register(modEventBus);
        // 贵族强化的阵营判定：不能靠 @Mod.EventBusSubscriber（那样没装 J村 也会被 Forge 加载本类），
        // 必须放在这道判断之后手动挂。
        MinecraftForge.EVENT_BUS.register(NobleFactionEvents.class);
        LOGGER.info("[galtc2-jvt] 已启用 JV Tinkers 内容：联动合金 / 物品 / 流体 / 强化 / 创造栏。");
    }

    /** 原 jvt 的 commonSetup：确认 J村法术体系（宝石波）是否就绪。未装 J村 时整段跳过。 */
    public static void commonSetup(FMLCommonSetupEvent event) {
        if (!JerotesCompat.isJerotesLoaded()) {
            return;
        }
        LOGGER.info("[galtc2-jvt] JV Tinkers 装配完成; gemstone_waves spell registered: {}",
                com.jerotes.jerotes.spell.SpellRegistry.spellExists("jerotesvillage_gemstone_waves"));
    }

    public static ResourceLocation loc(String s) {
        return ResourceLocation.fromNamespaceAndPath(MODID, s);
    }
}
