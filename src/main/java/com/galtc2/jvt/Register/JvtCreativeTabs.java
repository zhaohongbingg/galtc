package com.galtc2.jvt.Register;

import com.galtc2.jvt.Jvt;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.mantle.registration.object.MetalItemObject;

/**
 * JV Tinkers 页的物品列表。
 * <p>
 * 这里只列 JV 自己的 3 种金属 —— 每种按「锭 → 金属块 → 粒」三件套展开。
 * 熔融金属桶统一放在 galtc 页的「溶液桶」段，两个页面都扫注册表会重复，所以本页不再列桶。
 */
public class JvtCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Jvt.MODID);

    /** 本模组专属物品栏 */
    public static final RegistryObject<CreativeModeTab> JVT_TAB = CREATIVE_TABS.register("jvt",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.jvt"))
                    .icon(() -> new ItemStack(JvtMetal.ancient_second_rounder.getIngot()))
                    .displayItems((params, output) -> {
                        for (MetalItemObject metal : JvtMetal.ALL_METALS) {
                            output.accept(metal.getIngot());
                            output.accept(metal.get());
                            output.accept(metal.getNugget());
                        }
                    })
                    .build());

    public static void register() {
    }
}
