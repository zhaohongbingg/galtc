package com.galtc2.jvt.Register;

import com.galtc2.jvt.Jvt;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册位。
 * 示例材料 villager_metal 直接复用 J村自带的 jerotesvillage:villager_metal_ingot；
 * 古二轮金属锭为 jvt 自有物品。
 */
public class JvtItem {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Jvt.MODID);

    /** 古二轮金属锭 */
    public static final RegistryObject<Item> ancient_second_rounder_ingot =
            ITEMS.register("ancient_second_rounder_ingot", () -> new Item(new Item.Properties()));

    /** 火焰使徒锭 */
    public static final RegistryObject<Item> fire_apostle_ingot =
            ITEMS.register("fire_apostle_ingot", () -> new Item(new Item.Properties()));

    /** 镇山锭 */
    public static final RegistryObject<Item> mountain_realm_ingot =
            ITEMS.register("mountain_realm_ingot", () -> new Item(new Item.Properties()));

    public static void register() {
    }
}
