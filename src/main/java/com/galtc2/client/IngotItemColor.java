package com.galtc2.client;

import com.galtc2.Register.galtcItem;
import com.galtc2.Register.galtcMetal;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.mantle.registration.object.MetalItemObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 材料金属的着色：金属块 / 锭 / 粒共用一张灰度贴图，颜色由这里按材料名查表决定。
 * <p>
 * 三种形态的 tintindex 都是 0（锭/粒在 {@code item/generated} 的 layer0，
 * 金属块在 {@code models/block/<名>_block.json} 的每个面上），
 * 所以同一套配色能直接套给三件套。
 * <p>
 * 形态名与材料名的对应靠「去掉后缀」得到 —— 翻译键分别是
 * {@code item.galtc2.<名>_ingot}、{@code item.galtc2.<名>_nugget}、
 * {@code block.galtc2.<名>_block}（金属块的物品是 BlockItem，翻译键走 {@code block.} 前缀）。
 */
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
        Map.entry("morgana", 0xFF6A4C93),
        Map.entry("tianjin_zui", 0xFF9E8A9D),
        // 阴影：三件套用的是自己画的彩色贴图（深海军蓝 + 琥珀金边），
        // 这里给白色（= 不染色），让 tintindex 0 之后贴图保持原色。
        Map.entry("shaday", 0xFFFFFFFF)
    );

    /** 三件套的 id 后缀，顺序无关（只会命中一个）。 */
    private static final String[] FORM_SUFFIXES = {"_ingot", "_nugget", "_block"};

    private static final int WHITE = 0xFFFFFFFF;

    /**
     * 从翻译键还原材料名。
     * <pre>
     *   item.galtc2.narcissus_ingot   -> narcissus
     *   item.galtc2.narcissus_nugget  -> narcissus
     *   block.galtc2.narcissus_block  -> narcissus
     * </pre>
     * 取最后一段是为了同时兼容 {@code item.} 与 {@code block.} 两种前缀。
     */
    private static String materialKey(String descriptionId) {
        int dot = descriptionId.lastIndexOf('.');
        String path = dot >= 0 ? descriptionId.substring(dot + 1) : descriptionId;
        for (String suffix : FORM_SUFFIXES) {
            if (path.endsWith(suffix)) {
                return path.substring(0, path.length() - suffix.length());
            }
        }
        return path;
    }

    /** 物品栏 / 手持：锭、粒，以及金属块的 BlockItem。 */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColor colorProvider = (stack, tintIndex) ->
                tintIndex == 0 ? COLOR_MAP.getOrDefault(materialKey(stack.getItem().getDescriptionId()), WHITE) : WHITE;

        List<Item> items = new ArrayList<>(galtcMetal.ALL_METALS.size() * 3 + 1);
        for (MetalItemObject metal : galtcMetal.ALL_METALS) {
            items.add(metal.getIngot());
            items.add(metal.getNugget());
            items.add(metal.get().asItem());
        }
        // 万花镜不在三件套里（它不是匠魂材料），但贴图同样靠 tintindex 0 上色，
        // materialKey 对没有形态后缀的 id 会原样返回，所以直接加进来即可。
        items.add(galtcItem.kaleidoscope.get());
        event.register(colorProvider, items.toArray(new Item[0]));
    }

    /** 世界里的金属块：颜色来自方块模型面上写的 tintindex 0。 */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColor blockColor = (state, level, pos, tintIndex) ->
                tintIndex == 0 ? COLOR_MAP.getOrDefault(materialKey(state.getBlock().getDescriptionId()), WHITE) : WHITE;

        List<Block> blocks = new ArrayList<>(galtcMetal.ALL_METALS.size());
        for (MetalItemObject metal : galtcMetal.ALL_METALS) {
            blocks.add(metal.get());
        }
        event.register(blockColor, blocks.toArray(new Block[0]));
    }
}
