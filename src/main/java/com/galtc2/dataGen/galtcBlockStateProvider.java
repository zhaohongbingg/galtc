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
            // ore_generator 使用自定义 blockstate/模型（lit 状态），已有手动资源，跳过生成
            if (block == galtcBlock.ORE_GENERATOR) continue;
            simpleBlock(block.get());
        }
    }
}
