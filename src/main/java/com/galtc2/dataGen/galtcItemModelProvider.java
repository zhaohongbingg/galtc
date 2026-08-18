package com.galtc2.dataGen;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtcBlock;
import com.galtc2.Register.galtcItem;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class galtcItemModelProvider extends ItemModelProvider {

    public galtcItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, Galtc2.MODID, helper);
    }

    @Override
    protected void registerModels() {
        // 方块物品模型（继承方块模型）
        for (RegistryObject<Block> block : galtcBlock.BLOCKS.getEntries()) {
            withExistingParent(block.getId().getPath(), modLoc("block/" + block.getId().getPath()));
        }

        // 只为新物品生成模型（其他物品已有手动模型）
        basicItem(galtcItem.gal_core.get());
        basicItem(galtcItem.gal_record.get());
        basicItem(galtcItem.juqing_fragment.get());
        basicItem(galtcItem.seqi_fragment.get());
        basicItem(galtcItem.feimeng_fragment.get());
        basicItem(galtcItem.guro_fragment.get());
    }
}
