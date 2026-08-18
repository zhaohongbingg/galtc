package com.galtc2.Register;

import com.galtc2.Galtc2;
import com.galtc2.content.machine.OreGeneratorBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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

    public static final RegistryObject<Block> GAL_ORE =
            registerBlocks("gal_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops(), UniformInt.of(5, 10)));

    /** 剧情矿石 */
    public static final RegistryObject<Block> JUQING_ORE =
            registerBlocks("juqing_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops(), UniformInt.of(5, 10)));

    /** 涩气矿石 */
    public static final RegistryObject<Block> SEQI_ORE =
            registerBlocks("seqi_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops(), UniformInt.of(5, 10)));

    /** 废萌矿石 */
    public static final RegistryObject<Block> FEIMENG_ORE =
            registerBlocks("feimeng_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops(), UniformInt.of(5, 10)));

    /** guro矿石 */
    public static final RegistryObject<Block> GURO_ORE =
            registerBlocks("guro_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops(), UniformInt.of(5, 10)));

    /** 矿石生成器 */
    public static final RegistryObject<Block> ORE_GENERATOR =
            registerBlocks("ore_generator", OreGeneratorBlock::new);

    public static void register() {
    }
}
