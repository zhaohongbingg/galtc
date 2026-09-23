package com.galtc2.dataGen;

import com.galtc2.Register.galtcBlock;
import com.galtc2.Register.galtcItem;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class galtcBlockLootProvider extends BlockLootSubProvider {

    public galtcBlockLootProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        dropSelf(galtcBlock.GAL_CRYSTAL_BLOCK.get());
        dropSelf(galtcBlock.ORE_GENERATOR.get());
        // 晶簇和晶芽挖掉正常掉自己；只有母岩必须用精准采集，否则什么都不掉
        // 挖晶簇正常掉 4 个 Gal水晶（带精准采集则掉晶簇本身）
        add(galtcBlock.GAL_CORE.get(), createSilkTouchDispatchTable(galtcBlock.GAL_CORE.get(),
                applyExplosionDecay(galtcBlock.GAL_CORE.get(),
                        LootItem.lootTableItem(galtcItem.gal_crystal.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(4))))));
        dropSelf(galtcBlock.SMALL_GAL_CORE_BUD.get());
        dropSelf(galtcBlock.MEDIUM_GAL_CORE_BUD.get());
        dropSelf(galtcBlock.LARGE_GAL_CORE_BUD.get());
        add(galtcBlock.BUDDING_GAL_CORE.get(), createSilkTouchOnlyTable(galtcBlock.BUDDING_GAL_CORE.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return galtcBlock.BLOCKS.getEntries().stream()
                .map(RegistryObject::get)::iterator;
    }
}
