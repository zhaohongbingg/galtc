package com.galtc2.dataGen;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtcBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class galtcBlockStateProvider extends BlockStateProvider {

    public galtcBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, Galtc2.MODID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (RegistryObject<Block> block : galtcBlock.BLOCKS.getEntries()) {
            // 手写 blockstate/模型的方块（矿石生成器、Gal 晶簇/晶芽/母岩）跳过，别再按 cube_all 生成
            if (galtcBlock.HAND_MODELED.contains(block)) continue;
            simpleBlock(block.get());
        }
    }
}
