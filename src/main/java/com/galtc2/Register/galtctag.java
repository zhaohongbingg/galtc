package com.galtc2.Register;


import com.galtc2.Galtc2;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;



public class galtctag {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB_DEFERRED_REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Galtc2.MODID);

    public static final RegistryObject<CreativeModeTab> galtctab =
            CREATIVE_MODE_TAB_DEFERRED_REGISTER.register("galtc2",()-> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(galtcItem.sprb.get()))
                    .title(Component.translatable("itemGroup.galtctab"))
                    .displayItems((pParameters,pOutput)->{
                        // ---- 1. 锭 / Gal水晶 / 碎片（顺序见 galtcItem.ALL_ITEMS）----
                        galtcItem.ALL_ITEMS.forEach(item->
                                pOutput.accept(item.get()));

                        // ---- 2. 方块：水晶块 -> 矿石生成器 -> 晶簇 -> 晶芽 -> 母岩 ----
                        pOutput.accept(galtcBlock.GAL_CRYSTAL_BLOCK.get());
                        // 矿石生成器暂时停用（注释保留，未删除）
                        // pOutput.accept(galtcBlock.ORE_GENERATOR.get());
                        pOutput.accept(galtcBlock.GAL_CORE.get());
                        pOutput.accept(galtcBlock.SMALL_GAL_CORE_BUD.get());
                        pOutput.accept(galtcBlock.MEDIUM_GAL_CORE_BUD.get());
                        pOutput.accept(galtcBlock.LARGE_GAL_CORE_BUD.get());
                        pOutput.accept(galtcBlock.BUDDING_GAL_CORE.get());

                        // 熔融溶液桶不在创造栏列出：需要时用桶从冶炼炉 / 熔铸炉接，
                        // 或在 JEI 里搜 "molten" / 搜材料名查看。
                    })
                    .build());



public static void register(){

}


}
