package com.galtc2.dataGen;

import com.galtc2.Register.galtcBlock;
import com.galtc2.Register.galtcItem;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class galtcBlockLootProvider extends BlockLootSubProvider {

    public galtcBlockLootProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        dropOther(galtcBlock.GAL_ORE.get(), galtcItem.gal_core.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return galtcBlock.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
