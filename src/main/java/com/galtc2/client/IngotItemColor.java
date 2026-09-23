package com.galtc2.client;

import com.galtc2.Register.galtcItem;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = "galtc2", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class IngotItemColor {

    private static final Map<String, Integer> COLOR_MAP = Map.ofEntries(
        Map.entry("sprb", 0xFF00BFFF),
        Map.entry("zeroone", 0xFFA08E5F),
        Map.entry("kagome", 0xFFF8F8FF),
        Map.entry("aosora", 0xFF0FFCFE),
        Map.entry("atri", 0xFFFCE6D8),
        Map.entry("apeiria", 0xFFFCF7E1),
        Map.entry("narcissus", 0xFFBF9DFD),
        Map.entry("ca", 0xFFFFD700),
        Map.entry("kaleidoscope", 0xFF214E66),
        Map.entry("steinsgate", 0xFFFFA500),
        Map.entry("azarashi", 0xFF87CEEB),
        Map.entry("nighttimesheep", 0xFF214E66),
        Map.entry("oumunnbyou", 0xFFFFB7C5),
        Map.entry("kisaragikurenai", 0xFFFF6961),
        Map.entry("blacklily", 0xFF000000),
        Map.entry("muramasa", 0xFF660000),
        Map.entry("shochou", 0xFFCDB29E),
        Map.entry("izumi_hiyori", 0xFFFFFF00),
        Map.entry("nishiki_asumi", 0xFFD8EFF5),
        Map.entry("qienidian", 0xFFB4B9DD),
        Map.entry("rewrite", 0xFFE9AD86),
        Map.entry("badwoman", 0xFFF9C9D3),
        Map.entry("galaxy_railroad", 0xFF0A1128),
        Map.entry("ciallo", 0xFFB6CFA5),
        Map.entry("tsukiyashiro_kisaki", 0xFFF2E7A1),
        Map.entry("saya", 0xFF1F4D2E),
        Map.entry("shoujing", 0xFF16131F),
        Map.entry("euthanasia", 0xFF8B0000),
        Map.entry("qingfeng", 0xFF9BA3AB),
        Map.entry("morgana", 0xFF6A4C93)
    );

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColor colorProvider = (stack, tintIndex) -> {
            if (tintIndex == 0) {
                String name = stack.getItem().getDescriptionId();
                // 从 descriptionId 提取物品名: item.galtc2.xxx -> xxx
                String key = name.replace("item.galtc2.", "");
                return COLOR_MAP.getOrDefault(key, 0xFFFFFFFF);
            }
            return 0xFFFFFFFF;
        };

        event.register(colorProvider,
            galtcItem.sprb.get(),
            galtcItem.zeroone.get(),
            galtcItem.kagome.get(),
            galtcItem.aosora.get(),
            galtcItem.atri.get(),
            galtcItem.apeiria.get(),
            galtcItem.narcissus.get(),
            galtcItem.ca.get(),
            galtcItem.kaleidoscope.get(),
            galtcItem.steinsgate.get(),
            galtcItem.azarashi.get(),
            galtcItem.nighttimesheep.get(),
            galtcItem.oumunnbyou.get(),
            galtcItem.kisaragikurenai.get(),
            galtcItem.blacklily.get(),
            galtcItem.muramasa.get(),
            galtcItem.shochou.get(),
            galtcItem.izumi_hiyori.get(),
            galtcItem.nishiki_asumi.get(),
            galtcItem.qienidian.get(),
            galtcItem.rewrite.get(),
            galtcItem.badwoman.get(),
            galtcItem.galaxy_railroad.get(),
            galtcItem.ciallo.get(),
            galtcItem.tsukiyashiro_kisaki.get(),
            galtcItem.saya.get(),
            galtcItem.shoujing.get(),
            galtcItem.euthanasia.get(),
            galtcItem.qingfeng.get(),
            galtcItem.morgana.get()
        );
    }
}
