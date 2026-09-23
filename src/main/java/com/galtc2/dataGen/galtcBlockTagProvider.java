package com.galtc2.dataGen;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtcBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class galtcBlockTagProvider extends BlockTagsProvider {

    public galtcBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper helper) {
        super(output, lookupProvider, Galtc2.MODID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(galtcBlock.GAL_CRYSTAL_BLOCK.get())
                .add(galtcBlock.GAL_CORE.get())
                .add(galtcBlock.SMALL_GAL_CORE_BUD.get())
                .add(galtcBlock.MEDIUM_GAL_CORE_BUD.get())
                .add(galtcBlock.LARGE_GAL_CORE_BUD.get())
                .add(galtcBlock.BUDDING_GAL_CORE.get());

        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(galtcBlock.GAL_CRYSTAL_BLOCK.get());
    }
}
