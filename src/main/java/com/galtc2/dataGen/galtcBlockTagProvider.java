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
        // mineable/pickaxe 由手写文件 src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json 维护。
        // 这里不能再生成同名文件：generated 目录在 sourceSet 里排在 main 之后，会把手写版本整个盖掉，
        // 导致 33 个材料金属块丢失"镐可挖"标签（挖不动/不掉落）。
        // 新增方块时如果它 requiresCorrectToolForDrops()，记得同步加进那个手写文件
        // （ore_generator 就是漏加过，镐子挖不掉落）。只有联动模组在场才注册的方块
        // （ancient_second_rounder / fire_apostle / mountain_realm）写成 {"id": ..., "required": false}，
        // 免得没装联动模组时刷 "missing following references" 报错。

        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(galtcBlock.GAL_CRYSTAL_BLOCK.get());
    }
}
