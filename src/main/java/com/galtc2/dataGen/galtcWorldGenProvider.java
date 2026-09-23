package com.galtc2.dataGen;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtcBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class galtcWorldGenProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, galtcWorldGenProvider::ConfiguredFeatures)
            .add(Registries.PLACED_FEATURE, galtcWorldGenProvider::PlacedFeatures);

    public galtcWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, BUILDER, Set.of(Galtc2.MODID));
    }

    /** Gal 晶洞（野生生成，母岩的自然来源） */
    public static final ResourceKey<ConfiguredFeature<?, ?>> GAL_GEODE_CONFIGURED = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Galtc2.MODID, "gal_geode"));

    public static final ResourceKey<PlacedFeature> GAL_GEODE_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Galtc2.MODID, "gal_geode"));

    public static void ConfiguredFeatures(BootstapContext<ConfiguredFeature<?, ?>> context) {
        context.register(GAL_GEODE_CONFIGURED, new ConfiguredFeature<>(Feature.GEODE, galGeodeSettings()));
    }

    /**
     * Gal 晶洞的配置：数值照原版紫水晶晶洞那一套，只把方块换成我们自己的。
     * <p>
     * 内层整圈铺 <b>Gal 母岩</b>（所以野生晶洞里就能挖到母岩，而且它会继续往洞里长晶簇），
     * 中层方解石、外层 smooth basalt 做壳，洞里随机嵌 小/中/大晶芽 + Gal 晶簇（六个朝向都有）。
     */
    private static GeodeConfiguration galGeodeSettings() {
        List<BlockState> placements = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            for (Block bud : List.of(galtcBlock.SMALL_GAL_CORE_BUD.get(),
                    galtcBlock.MEDIUM_GAL_CORE_BUD.get(),
                    galtcBlock.LARGE_GAL_CORE_BUD.get())) {
                placements.add(bud.defaultBlockState().setValue(AmethystClusterBlock.FACING, direction));
            }
            placements.add(galtcBlock.GAL_CORE.get().defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, direction));
        }
        return new GeodeConfiguration(
                new GeodeBlockSettings(
                        BlockStateProvider.simple(Blocks.AIR),
                        BlockStateProvider.simple(galtcBlock.GAL_CRYSTAL_BLOCK.get()),
                        BlockStateProvider.simple(galtcBlock.BUDDING_GAL_CORE.get()),
                        BlockStateProvider.simple(Blocks.CALCITE),
                        BlockStateProvider.simple(Blocks.SMOOTH_BASALT),
                        placements,
                        BlockTags.FEATURES_CANNOT_REPLACE,
                        BlockTags.GEODE_INVALID_BLOCKS),
                new GeodeLayerSettings(1.7D, 2.2D, 3.2D, 4.2D),
                new GeodeCrackSettings(0.95D, 2.0D, 2),
                0.35D, 0.083D, true,
                UniformInt.of(4, 6),
                UniformInt.of(3, 4),
                UniformInt.of(1, 2),
                -16, 16, 0.05D, 1);
    }

    public static void PlacedFeatures(BootstapContext<PlacedFeature> context) {
        // 平均每 24 个区块一个，y = -58 ~ 30（和原版紫水晶晶洞同一档稀有度）
        context.register(GAL_GEODE_PLACED, new PlacedFeature(
                context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(GAL_GEODE_CONFIGURED),
                List.of(
                        RarityFilter.onAverageOnceEvery(24),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(-58), VerticalAnchor.absolute(30)),
                        BiomeFilter.biome()
                )));
    }
}
