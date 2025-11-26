package com.galtc2.Register;


import com.galtc2.Galtc2;
import net.minecraft.world.item.Item;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class galtcItem {
    public static final DeferredRegister<Item> ITEMS=
            DeferredRegister.create(ForgeRegistries.ITEMS, Galtc2.MODID);
//夏日锭
    public  static  final   RegistryObject<Item> sprb =
            ITEMS.register("sprb",()->new Item(new Item.Properties()));

    //零一锭
    public static final RegistryObject<Item> zeroone =
            ITEMS.register("zeroone",()->new Item(new Item.Properties()));
    public static  final RegistryObject<Item> kagome =
            ITEMS.register("kagome",()->new Item(new Item.Properties()));//笼女
    public static  final RegistryObject<Item>  aosora=
            ITEMS.register("aosora",()->new Item(new Item.Properties()));//苍彼
    public  static  final RegistryObject<Item> kagari=
            ITEMS.register("kagari",()->new Item(new Item.Properties()));//篝
    public static final RegistryObject<Item> atri=
            ITEMS.register("atri",()->new Item(new Item.Properties()));//arti
    public static final RegistryObject<Item> apeiria=
            ITEMS.register("apeiria",()->new Item(new Item.Properties()));//艾佩莉雅
    public  static  final  RegistryObject<Item> einstein=
            ITEMS.register("einstein",()->new Item(new Item.Properties()));// 爱因斯坦挟爱之上
    public  static  final  RegistryObject<Item> narcissus=
            ITEMS.register("narcissus",()->new Item(new Item.Properties()));//水仙
    public  static  final  RegistryObject<Item> ca =
            ITEMS.register("ca",()->new Item(new Item.Properties())); //CA(水葬）

    public static  final RegistryObject<Item> kaleidoscope=
            ITEMS.register("kaleidoscope",()->new Item(new Item.Properties()));//kaleidoscope

    public static  final RegistryObject<Item> steinsgate=
            ITEMS.register("steinsgate",()->new Item(new Item.Properties()));//steinsGate
    public static  final RegistryObject<Item> azarashi=
            ITEMS.register("azarashi",()->new Item(new Item.Properties()));//azarashi
    public static  final RegistryObject<Item> nighttimesheep=
            ITEMS.register("nighttimesheep",()->new Item(new Item.Properties()));//夜时羊
    public static  final RegistryObject<Item> oumunnbyou=
                ITEMS.register("oumunnbyou",()->new Item(new Item.Properties()));//生命的备件
    public static  final RegistryObject<Item> kisaragikurenai=
            ITEMS.register("kisaragikurenai",()->new Item(new Item.Properties()));//入木三分
    public static  final RegistryObject<Item> blacklily=
            ITEMS.register("blacklily",()->new Item(new Item.Properties()));//黑百合
    public static  final RegistryObject<Item> muramasa=
            ITEMS.register("muramasa",()->new Item(new Item.Properties()));//村正


    public static final List<RegistryObject<Item>> ALL_ITEMS = List.of(
            sprb,
            zeroone,
            kagome,
            aosora,
            kagari,
            atri,
            apeiria,
            einstein,
            narcissus,
            ca,
            kaleidoscope,
            steinsgate,
            azarashi,
            nighttimesheep,
            oumunnbyou,
            kisaragikurenai,
            blacklily,
            muramasa
    );







    public static void register(){
    }
}
