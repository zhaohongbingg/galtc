package com.galtc2.Register;

import com.galtc2.Galtc2;
import com.galtc2.content.block.BuddingGalCoreBlock;
import com.galtc2.content.machine.OreGeneratorBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Supplier;

public class galtcBlock {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Galtc2.MODID);

    private static <T extends Block> void registerBlockItems(String name, RegistryObject<T> block) {
        galtcItem.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Block> RegistryObject<T> registerBlocks(String name, Supplier<T> block) {
        RegistryObject<T> registered = BLOCKS.register(name, block);
        registerBlockItems(name, registered);
        return registered;
    }

    /** 矿石生成器（功能暂时停用：见 OreGeneratorBlock#getTicker，注册与资源都还在） */
    public static final RegistryObject<Block> ORE_GENERATOR =
            registerBlocks("ore_generator", OreGeneratorBlock::new);

    /**
     * Gal 水晶块：水晶的储存 / 装饰方块，对应匠魂的 {@code *_slime_crystal_block}。
     * <p>
     * 直接继承原版 {@link AmethystBlock}，所以敲一下会响（和紫水晶块一样）。
     */
    public static final RegistryObject<Block> GAL_CRYSTAL_BLOCK =
            registerBlocks("gal_crystal_block", () -> new AmethystBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(1.5F)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()));

    /**
     * Gal 晶簇（原来的 Gal 核心物品改成了方块）。
     * <p>
     * 参数照原版紫水晶簇：高度 7、水平偏移 3、发光 5，可以朝向六个面、可以含水。
     */
    public static final RegistryObject<Block> GAL_CORE =
            registerBlocks("gal_core", () -> new AmethystClusterBlock(7, 3,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PINK)
                            .forceSolidOn()
                            .noOcclusion()
                            .sound(SoundType.AMETHYST_CLUSTER)
                            .strength(1.5F)
                            .lightLevel(state -> 5)
                            .pushReaction(PushReaction.DESTROY)));

    /** 小晶芽（高度 3、偏移 4、发光 1） */
    public static final RegistryObject<Block> SMALL_GAL_CORE_BUD =
            registerBlocks("small_gal_core_bud", () -> new AmethystClusterBlock(3, 4,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PINK)
                            .forceSolidOn()
                            .noOcclusion()
                            .sound(SoundType.SMALL_AMETHYST_BUD)
                            .strength(1.5F)
                            .lightLevel(state -> 1)
                            .pushReaction(PushReaction.DESTROY)));

    /** 中晶芽（高度 4、偏移 3、发光 2） */
    public static final RegistryObject<Block> MEDIUM_GAL_CORE_BUD =
            registerBlocks("medium_gal_core_bud", () -> new AmethystClusterBlock(4, 3,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PINK)
                            .forceSolidOn()
                            .noOcclusion()
                            .sound(SoundType.MEDIUM_AMETHYST_BUD)
                            .strength(1.5F)
                            .lightLevel(state -> 2)
                            .pushReaction(PushReaction.DESTROY)));

    /** 大晶芽（高度 5、偏移 3、发光 4） */
    public static final RegistryObject<Block> LARGE_GAL_CORE_BUD =
            registerBlocks("large_gal_core_bud", () -> new AmethystClusterBlock(5, 3,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PINK)
                            .forceSolidOn()
                            .noOcclusion()
                            .sound(SoundType.LARGE_AMETHYST_BUD)
                            .strength(1.5F)
                            .lightLevel(state -> 4)
                            .pushReaction(PushReaction.DESTROY)));

    /** Gal 母岩：随机刻慢慢长出晶芽，最终长成 Gal 晶簇 */
    public static final RegistryObject<Block> BUDDING_GAL_CORE =
            registerBlocks("budding_gal_core", () -> new BuddingGalCoreBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PINK)
                            .strength(1.5F)
                            .randomTicks()
                            .sound(SoundType.AMETHYST)));

    /**
     * blockstate / 方块模型是手写的（晶簇和晶芽是 cross 模型、母岩是自定义模型），
     * datagen 不要给它们按 cube_all 生成模型。
     */
    public static final List<RegistryObject<Block>> HAND_MODELED = List.of(
            ORE_GENERATOR,
            GAL_CORE, SMALL_GAL_CORE_BUD, MEDIUM_GAL_CORE_BUD, LARGE_GAL_CORE_BUD, BUDDING_GAL_CORE);
    // 注：矿石生成器暂时停用时，它的 blockstate / 模型仍留在资源里（未删除），所以这里继续排除 datagen。

    public static void register() {
    }
}
