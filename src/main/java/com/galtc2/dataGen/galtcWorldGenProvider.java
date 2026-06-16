package com.galtc2.dataGen;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtcBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;

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

    public static final ResourceKey<ConfiguredFeature<?, ?>> GAL_ORE_CONFIGURED = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, new ResourceLocation(Galtc2.MODID, "gal_ore"));

    public static final ResourceKey<PlacedFeature> GAL_ORE_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE, new ResourceLocation(Galtc2.MODID, "gal_ore"));

    public static void ConfiguredFeatures(BootstapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest deepslateReplaceable = new TagMatchTest(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD);

        List<OreConfiguration.TargetBlockState> targets = List.of(
                OreConfiguration.target(deepslateReplaceable, galtcBlock.GAL_ORE.get().defaultBlockState())
        );

        context.register(GAL_ORE_CONFIGURED, new ConfiguredFeature<>(
                Feature.ORE, new OreConfiguration(targets, 2, 0.0F)));
    }

    public static void PlacedFeatures(BootstapContext<PlacedFeature> context) {
        context.register(GAL_ORE_PLACED, new PlacedFeature(
                context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(GAL_ORE_CONFIGURED),
                List.of(
                        CountPlacement.of(1),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(-50), VerticalAnchor.absolute(-30)),
                        BiomeFilter.biome()
                )));
    }
}
