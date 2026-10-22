package com.galtc2.client.tooltip;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 提示框自定义图片的登记表（客户端）。
 * <p>
 * 数据来自资源包里的 {@code assets/<modid>/tooltip_images/*.json}：
 * <pre>
 * {
 *   "items": ["galtc2:ciallo"],                         // 也可以写单个的 "item"
 *   "texture": "galtc2:textures/gui/tooltip/xxx.png",   // 完整纹理路径
 *   "color": "FF9E8A9D",                                // 可省略，默认白色（原样输出）
 *   "position": "top"                                   // 可省略，默认 bottom（图片在文字下面）
 * }
 * </pre>
 * 尺寸自动按 PNG 的<b>真实像素</b>算，并且按比例分成三类，同类占的提示框大小完全一致：
 * <ul>
 *   <li>横屏 → {@link #LANDSCAPE_WIDTH}×{@link #LANDSCAPE_HEIGHT}（7:4，素材大多在 1.6～1.85）</li>
 *   <li>方形 → {@link #SQUARE_WIDTH}×{@link #SQUARE_HEIGHT}（1:1，接近正方形的图，例如 saya）</li>
 *   <li>竖屏 → {@link #PORTRAIT_WIDTH}×{@link #PORTRAIT_HEIGHT}（3:4）</li>
 * </ul>
 * 每张图自动归到<b>能显示得最大</b>的那一类（见 {@link #pickCanvas}），不用手写比例阈值。
 * 原图只做<b>等比缩放</b>缩进画布并居中（不拉伸、不变形，比例不吻合时差的部分是透明的），
 * 所以不用在 JSON 里写尺寸；想给某一张单独指定画布再写 {@code width}/{@code height}。
 * <p>
 * 跟随资源包重载（{@code F3+T} 或换包）重新读取，所以换图片只要替换 PNG，不用改代码。
 */
public class TooltipImageRegistry extends SimpleJsonResourceReloadListener {

    /** 资源包目录：assets/&lt;modid&gt;/tooltip_images/ */
    public static final String DIRECTORY = "tooltip_images";

    /**
     * 横屏图片统一使用的显示画布（逻辑像素，比例 7:4）。
     * <p>
     * 定成 224 宽是因为素材基本在 850～1900 像素宽：GUI 缩放 4 时 224×4 = 896 个屏幕像素，
     * 和原图接近 1:1，细节最清楚；嫌大/嫌小直接改这几个数。
     */
    public static final int LANDSCAPE_WIDTH = 224;
    public static final int LANDSCAPE_HEIGHT = 128;

    /** 方形图片（宽高比接近 1）统一使用的显示画布（逻辑像素，比例 1:1） */
    public static final int SQUARE_WIDTH = 152;
    public static final int SQUARE_HEIGHT = 152;

    /** 竖屏图片统一使用的显示画布（逻辑像素，比例 3:4） */
    public static final int PORTRAIT_WIDTH = 120;
    public static final int PORTRAIT_HEIGHT = 160;

    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TooltipImageRegistry INSTANCE = new TooltipImageRegistry();

    /** 物品 -> 要插入提示框的图片 */
    private static final Map<Item, Entry> BY_ITEM = new HashMap<>();

    /**
     * @param component 图片组件（原版会负责排版与绘制）
     * @param top       true = 插在提示框最上面，false = 插在最后
     */
    public record Entry(TooltipImageComponent component, boolean top) {
    }

    private TooltipImageRegistry() {
        super(GSON, DIRECTORY);
    }

    /** 注册到 {@code RegisterClientReloadListenersEvent} 的那个实例 */
    public static TooltipImageRegistry get() {
        return INSTANCE;
    }

    /** 取某个物品要插入提示框的图片，没配置则返回 null */
    @Nullable
    public static Entry get(ItemStack stack) {
        return stack.isEmpty() ? null : BY_ITEM.get(stack.getItem());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        BY_ITEM.clear();
        for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
            ResourceLocation id = file.getKey();
            try {
                JsonObject json = GsonHelper.convertToJsonObject(file.getValue(), id.toString());
                ResourceLocation texture = ResourceLocation.tryParse(GsonHelper.getAsString(json, "texture"));
                if (texture == null) {
                    LOGGER.warn("[galtc2] 提示框图片 {} 的 texture 不是合法路径。", id);
                    continue;
                }
                // 尺寸自动来：读 PNG 真实像素 → 按横屏/竖屏归到统一画布 → 等比缩进画布居中
                int[] natural = readTextureSize(manager, texture, id);
                if (natural == null) {
                    continue;
                }
                int[] canvas = pickCanvas(natural);
                int boxWidth = GsonHelper.getAsInt(json, "width", canvas[0]);
                int boxHeight = GsonHelper.getAsInt(json, "height", canvas[1]);
                if (boxWidth <= 0 || boxHeight <= 0) {
                    LOGGER.warn("[galtc2] 提示框图片 {} 的尺寸不合法：{}x{}。", id, boxWidth, boxHeight);
                    continue;
                }
                int[] shown = fitInside(natural, boxWidth, boxHeight);
                // 缩得比较狠的用线性过滤，否则降采样会出斑点；接近 1:1 的保持 NEAREST 更锐利
                boolean smooth = natural[0] >= shown[0] * 2 || natural[1] >= shown[1] * 2;
                boolean top = "top".equalsIgnoreCase(GsonHelper.getAsString(json, "position", "bottom"));
                int color = parseColor(GsonHelper.getAsString(json, "color", "FFFFFFFF"), id);
                TooltipImageComponent component = new TooltipImageComponent(texture,
                        boxWidth, boxHeight, shown[0], shown[1], smooth, color);
                for (Item item : parseItems(json)) {
                    BY_ITEM.put(item, new Entry(component, top));
                }
            } catch (Exception e) {
                LOGGER.error("[galtc2] 解析提示框图片 {} 失败：{}", id, e.toString());
            }
        }
        LOGGER.info("[galtc2] 提示框图片配置加载完成，共 {} 个物品。", BY_ITEM.size());
    }

    /**
     * 把图片归到三类画布之一：横屏 7:4 / 方形 1:1 / 竖屏 3:4。
     * <p>
     * 判据是<b>哪个画布能让这张图显示得最大</b>（比的是缩放后的像素面积），所以不用手写比例阈值：
     * 接近正方形的图（例如 759×712 的 saya、786×876 的 qingfeng）会落到方形，
     * 而 1.3 左右的 4:3 图仍然留在横屏——那边虽然比例不合，但给出来的图更大。
     */
    private static int[] pickCanvas(int[] natural) {
        int[][] canvases = {
                {LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT},
                {SQUARE_WIDTH, SQUARE_HEIGHT},
                {PORTRAIT_WIDTH, PORTRAIT_HEIGHT}
        };
        int[] best = canvases[0];
        int bestArea = -1;
        for (int[] canvas : canvases) {
            int[] shown = fitInside(natural, canvas[0], canvas[1]);
            int area = shown[0] * shown[1];
            if (area > bestArea) {
                bestArea = area;
                best = canvas;
            }
        }
        return best;
    }

    /**
     * 把原图<b>等比</b>缩进画布：<b>只缩不放</b>（小图保持原样不会被放大变糊，大图也不会撑满屏幕），
     * 比例和画布不一致时按较长的一边贴合，剩下的部分由 {@link TooltipImageComponent} 居中留白。
     *
     * @return {@code [实际绘制宽, 实际绘制高]}，至少 1×1
     */
    private static int[] fitInside(int[] natural, int boxWidth, int boxHeight) {
        double scale = Math.min(1.0D, Math.min(
                (double) boxWidth / natural[0], (double) boxHeight / natural[1]));
        return new int[]{
                Math.max(1, (int) Math.round(natural[0] * scale)),
                Math.max(1, (int) Math.round(natural[1] * scale))
        };
    }

    /**
     * 读 PNG 的<b>真实像素</b>尺寸。
     *
     * @return {@code [宽, 高]}；纹理不存在或不是能解析的 PNG 时返回 null（该条目会被跳过）
     */
    private static int[] readTextureSize(ResourceManager manager, ResourceLocation texture, ResourceLocation configId) {
        try {
            Optional<Resource> resource = manager.getResource(texture);
            if (resource.isEmpty()) {
                LOGGER.warn("[galtc2] 提示框图片 {} 引用的纹理不存在：{}", configId, texture);
                return null;
            }
            try (InputStream in = resource.get().open()) {
                NativeImage image = NativeImage.read(in);
                try {
                    return new int[]{image.getWidth(), image.getHeight()};
                } finally {
                    image.close();
                }
            }
        } catch (IOException e) {
            LOGGER.error("[galtc2] 读取提示框图片 {} 的纹理尺寸失败（{}）：{}", configId, texture, e.toString());
            return null;
        }
    }

    /**
     * 解析 {@code color}：染色用的 ARGB 十六进制。
     * <p>
     * 接受 {@code "FF9E8A9D"}（带 alpha）、{@code "9E8A9D"}（不带，自动补满不透明）
     * 以及带 {@code #} 前缀的写法；缺省或写坏时用白色，也就是原样输出不染色。
     * <p>
     * 用法：素材做成<b>灰度</b>图，各材料只改这里的颜色 —— 一张图配 N 种颜色，
     * 不用给每个材料单独画一张彩色图。和锭/粒/块走 {@code IngotItemColor} 是同一个思路。
     */
    private static int parseColor(String raw, ResourceLocation configId) {
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        try {
            long value = Long.parseLong(hex, 16);
            // 只写了 RGB 时补上不透明的 alpha
            return hex.length() <= 6 ? (int) (value | 0xFF000000L) : (int) value;
        } catch (NumberFormatException e) {
            LOGGER.warn("[galtc2] 提示框图片 {} 的颜色 {} 不是合法的十六进制，改用白色。", configId, raw);
            return TooltipImageComponent.WHITE;
        }
    }

    private static List<Item> parseItems(JsonObject json) {
        List<Item> items = new ArrayList<>();
        if (json.has("items")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "items")) {
                addItem(items, element.getAsString());
            }
        } else {
            addItem(items, GsonHelper.getAsString(json, "item"));
        }
        return items;
    }

    private static void addItem(List<Item> items, String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        Item item = loc == null ? null : ForgeRegistries.ITEMS.getValue(loc);
        if (item == null || item == Items.AIR) {
            LOGGER.warn("[galtc2] 提示框图片配置里的物品不存在：{}", id);
            return;
        }
        items.add(item);
    }
}
