package com.galtc2.Register;


import com.galtc2.Galtc2;
import net.minecraft.world.item.Item;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * 非金属类的物品注册位。
 * <p>
 * <b>30 种材料锭已经迁移到 {@link galtcMetal}</b>：那边用匠魂官方的
 * {@code BlockDeferredRegister#registerMetal} 注册，一种材料一次拿到
 * 「金属块 / 锭 / 粒」三件套，id 也从 {@code galtc2:<名>} 变成了 {@code galtc2:<名>_ingot}。
 * 这里只留水晶与矿石碎片这类零散物品。
 * <p>
 * 方块物品（Gal 水晶块、晶簇等）由 {@link galtcBlock#registerBlocks} 注册进本类的
 * {@link #ITEMS}，所以这个注册器仍然要挂到 mod 事件总线上。
 */
public class galtcItem {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Galtc2.MODID);

    // 挖晶簇掉的水晶
    public static final RegistryObject<Item> gal_crystal =
            ITEMS.register("gal_crystal", () -> new Item(new Item.Properties()));

    // 矿石碎片
    public static final RegistryObject<Item> juqing_fragment =
            ITEMS.register("juqing_fragment", () -> new Item(new Item.Properties()));//剧情碎片
    public static final RegistryObject<Item> seqi_fragment =
            ITEMS.register("seqi_fragment", () -> new Item(new Item.Properties()));//涩气碎片
    public static final RegistryObject<Item> feimeng_fragment =
            ITEMS.register("feimeng_fragment", () -> new Item(new Item.Properties()));//废萌碎片
    public static final RegistryObject<Item> guro_fragment =
            ITEMS.register("guro_fragment", () -> new Item(new Item.Properties()));//guro碎片


    /**
     * 创造栏的展示顺序（<b>只影响显示</b>：物品的注册顺序 = 上面字段的声明顺序，
     * 也就是存档里的 id，改这里不会动到旧存档）。
     * <p>
     * 材料金属三件套不在这里，见 {@link galtcMetal#ALL_METALS} ——
     * 创造栏把它们排在前面，见 {@link galtctag}。
     */
    public static final List<RegistryObject<Item>> ALL_ITEMS = List.of(
            // ---- Gal水晶 与四种碎片 ----
            gal_crystal, juqing_fragment, seqi_fragment, feimeng_fragment, guro_fragment
    );


    public static void register() {
    }
}
