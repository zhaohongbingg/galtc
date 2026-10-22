package com.galtc2;

import com.galtc2.Register.*;
import com.galtc2.client.tooltip.TooltipImageRegistry;
import com.galtc2.compat.bomd.BomdCompat;
import com.galtc2.compat.bomd.client.BlazingEyeLaserRenderer;
import com.galtc2.compat.jerotes.JerotesCompat;
import com.galtc2.compat.meetyourfight.MeetYourFightCompat;
import com.galtc2.compat.relicward.RelicWardCompat;

// 暂时停用矿石生成器（注释保留，未删除）：恢复时放开本行与 commonSetup 里的配置加载
// import com.galtc2.content.machine.OreGeneratorConfig;
import com.galtc2.dataGen.*;
import com.galtc2.jvt.Jvt;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Galtc2.MODID)
public class Galtc2 {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "galtc2";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "galtc2" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "galtc2" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "galtc2:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    // Creates a new BlockItem with the id "galtc2:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEat().nutrition(1).saturationMod(2f).build())));

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    /**
     * 构造参数由 Forge 注入（javafml 语言加载器提供的上下文）。
     * 这是 {@code FMLJavaModLoadingContext.get()} 被标记为待删除后的官方写法，
     * Forge 会优先找这个带参构造函数，找不到才回退到无参构造。
     */
    public Galtc2(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        galtc2Fluid.FLUIDS.register(modEventBus);
        galtcItem.ITEMS.register(modEventBus);
        galtcBlock.BLOCKS.register(modEventBus);
        // 材料金属三件套（金属块 / 锭 / 粒）：BlockDeferredRegister 自带 block 与 item 两个注册表，
        // 这一行就把两边都挂上了。
        galtcMetal.BLOCKS.register(modEventBus);
        galtcBlockEntity.BLOCK_ENTITIES.register(modEventBus);
        galtc2Effects.EFFECTS.register(modEventBus);
        galtctag.CREATIVE_MODE_TAB_DEFERRED_REGISTER.register(modEventBus);
        galtc2Sounds.SOUND_EVENTS.register(modEventBus);
        galHooks.register();
        GaltcConfig.init(context);
        galtc2Modifier.MODIFIERS.register(modEventBus);
        // J村(Jerotes Village) 软联动：未安装时自动跳过
        JerotesCompat.init(modEventBus);
        // 迎战(Meet Your Fight) 软联动：未安装时自动跳过
        MeetYourFightCompat.init(modEventBus);
        // 祸乱鬼魅(Bosses of Mass Destruction) 软联动：未安装时自动跳过
        BomdCompat.init(modEventBus);
        // 遗境守望(Relic Ward) 软联动：未安装时自动跳过
        RelicWardCompat.init(modEventBus);
        // JV Tinkers（原 jvt 模组，已并入本模组，命名空间统一为 galtc2）
        Jvt.init(modEventBus);
        modEventBus.addListener(Jvt::commonSetup);
    }
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        // 矿石生成器暂时停用（注释保留，未删除）：恢复时放开上面那行 import 与下面这行
        // OreGeneratorConfig.load(FMLPaths.CONFIGDIR.get().toFile());
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
            // 祸乱鬼魅 联动：祐天寺美汐的实心光束（照 BOMD 的下界铁掌激光画）。
            // 这个类引用了 BOMD / cerbons_api 的类型，所以必须等确认装了 BOMD 之后再触碰它
            // —— import 语句本身不会触发类加载，只有走到下面这行才会。
            if (BomdCompat.isLoaded()) {
                BlazingEyeLaserRenderer.register();
            }
        }

        /** 提示框自定义图片：读 assets/<modid>/tooltip_images/*.json，随资源包重载刷新 */
        @SubscribeEvent
        public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(TooltipImageRegistry.get());
        }
    }
    public static ResourceLocation loc(String s) {
        return ResourceLocation.fromNamespaceAndPath(MODID, s);
    }
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {

        @SubscribeEvent
        public static void gatherData(GatherDataEvent event) {
            DataGenerator gen = event.getGenerator();
            PackOutput output = gen.getPackOutput();
            ExistingFileHelper helper = event.getExistingFileHelper();
            CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

            if (event.includeClient()) {
                gen.addProvider(true, new galtcFluidGen(output, helper));
                gen.addProvider(true, new galtcBlockStateProvider(output, helper));
                gen.addProvider(true, new galtcItemModelProvider(output, helper));
                gen.addProvider(true, new galtcBlockTagProvider(output, lookupProvider, helper));
            }
            if (event.includeServer()) {
                gen.addProvider(true, new galtcLootTableProvider(output));
                gen.addProvider(true, new galtcWorldGenProvider(output, lookupProvider));
            }
        }
    }
}
