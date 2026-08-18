package com.galtc2.content.machine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 加载配置目录下的矿石生成定义 JSON 文件。
 * <p>
 * 文件路径: config/galtc2/ore_generator.json
 */
public class OreGeneratorConfig {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static OreGeneratorConfig INSTANCE;

    private long energyCapacity = 500_000;
    private long energyPerTick = 1_000;
    private int ticksPerOperation = 100;
    private List<OreEntry> ores = new ArrayList<>();

    // ===== 加载 / 重新加载 =====

    public static void load(File configDir) {
        File dir = new File(configDir, "galtc2");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "ore_generator.json");
        if (!file.exists()) {
            // 首次运行：生成默认配置
            generateDefault(file);
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            OreGeneratorConfig cfg = fromJson(json);
            INSTANCE = cfg;
            LOGGER.info("已加载矿石生成器配置，共 {} 种矿石", cfg.ores.size());
        } catch (Exception e) {
            LOGGER.error("加载矿石生成器配置失败，使用默认值", e);
            INSTANCE = defaultConfig();
        }
    }

    // ===== 读取 =====

    public static OreGeneratorConfig get() {
        if (INSTANCE == null) {
            INSTANCE = defaultConfig();
        }
        return INSTANCE;
    }

    public long getEnergyCapacity() { return energyCapacity; }
    public long getEnergyPerTick()   { return energyPerTick; }
    public int  getTicksPerOperation() { return ticksPerOperation; }
    public List<OreEntry> getOres()  { return ores; }

    /**
     * 根据权重随机选一种矿石产出，返回 ItemStack
     */
    public ItemStack rollOre(Random random) {
        if (ores.isEmpty()) return ItemStack.EMPTY;

        int totalWeight = ores.stream().mapToInt(OreEntry::getWeight).sum();
        if (totalWeight <= 0) return ItemStack.EMPTY;

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (OreEntry entry : ores) {
            cumulative += entry.getWeight();
            if (roll < cumulative) {
                int count = entry.getMinCount() + random.nextInt(entry.getMaxCount() - entry.getMinCount() + 1);
                ItemStack stack = new ItemStack(entry.getItem(), count);
                if (!stack.isEmpty()) return stack;
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    // ===== 内部 =====

    private static OreGeneratorConfig fromJson(JsonObject json) {
        OreGeneratorConfig cfg = new OreGeneratorConfig();
        cfg.energyCapacity = getLong(json, "energyCapacity", 500_000);
        cfg.energyPerTick  = getLong(json, "energyPerTick", 1_000);
        cfg.ticksPerOperation = getInt(json, "ticksPerOperation", 100);

        var oresArray = json.getAsJsonArray("ores");
        if (oresArray != null) {
            Type listType = new TypeToken<List<OreEntryData>>() {}.getType();
            List<OreEntryData> dataList = GSON.fromJson(oresArray, listType);
            if (dataList != null) {
                for (OreEntryData d : dataList) {
                    Item item = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(d.item));
                    if (item == null || item == net.minecraft.world.item.Items.AIR) {
                        LOGGER.warn("未知物品: {}", d.item);
                        continue;
                    }
                    cfg.ores.add(new OreEntry(
                            item, d.weight, d.minCount, d.maxCount, d.energyPerOperation
                    ));
                }
            }
        }
        return cfg;
    }

    private static OreGeneratorConfig defaultConfig() {
        OreGeneratorConfig cfg = new OreGeneratorConfig();
        cfg.energyCapacity = 500_000;
        cfg.energyPerTick = 1_000;
        cfg.ticksPerOperation = 100;
        Item ironOre  = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse("minecraft:iron_ore"));
            Item goldOre  = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse("minecraft:gold_ore"));
            Item diamondOre = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse("minecraft:diamond_ore"));
            cfg.ores = new ArrayList<>();
            if (ironOre != null && ironOre != net.minecraft.world.item.Items.AIR)
                cfg.ores.add(new OreEntry(ironOre, 30, 1, 3, 40_000));
            if (goldOre != null && goldOre != net.minecraft.world.item.Items.AIR)
                cfg.ores.add(new OreEntry(goldOre, 20, 1, 2, 60_000));
            if (diamondOre != null && diamondOre != net.minecraft.world.item.Items.AIR)
                cfg.ores.add(new OreEntry(diamondOre, 5, 1, 1, 200_000));
        return cfg;
    }

    private static void generateDefault(File file) {
        String defaultJson = """
                {
                  "energyCapacity": 500000,
                  "energyPerTick": 1000,
                  "ticksPerOperation": 100,
                  "ores": [
                    { "item": "minecraft:coal_ore",       "weight": 40, "minCount": 1, "maxCount": 4, "energyPerOperation": 20000 },
                    { "item": "minecraft:iron_ore",        "weight": 30, "minCount": 1, "maxCount": 3, "energyPerOperation": 40000 },
                    { "item": "minecraft:gold_ore",        "weight": 20, "minCount": 1, "maxCount": 2, "energyPerOperation": 60000 },
                    { "item": "minecraft:redstone_ore",    "weight": 25, "minCount": 2, "maxCount": 5, "energyPerOperation": 30000 },
                    { "item": "minecraft:lapis_ore",       "weight": 20, "minCount": 2, "maxCount": 4, "energyPerOperation": 30000 },
                    { "item": "minecraft:diamond_ore",     "weight": 5,  "minCount": 1, "maxCount": 1, "energyPerOperation": 200000 },
                    { "item": "minecraft:emerald_ore",     "weight": 3,  "minCount": 1, "maxCount": 1, "energyPerOperation": 300000 },
                    { "item": "galtc2:gal_ore",            "weight": 10, "minCount": 1, "maxCount": 2, "energyPerOperation": 100000 },
                    { "item": "galtc2:juqing_ore",         "weight": 8,  "minCount": 1, "maxCount": 2, "energyPerOperation": 80000 },
                    { "item": "galtc2:seqi_ore",           "weight": 6,  "minCount": 1, "maxCount": 2, "energyPerOperation": 120000 },
                    { "item": "galtc2:feimeng_ore",        "weight": 10, "minCount": 1, "maxCount": 2, "energyPerOperation": 60000 },
                    { "item": "galtc2:guro_ore",           "weight": 4,  "minCount": 1, "maxCount": 1, "energyPerOperation": 150000 }
                  ]
                }
                """;
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(defaultJson);
        } catch (IOException e) {
            LOGGER.error("生成默认矿石生成器配置文件失败", e);
        }
    }

    private static long getLong(JsonObject json, String key, long def) {
        return json.has(key) ? json.get(key).getAsLong() : def;
    }
    private static int getInt(JsonObject json, String key, int def) {
        return json.has(key) ? json.get(key).getAsInt() : def;
    }

    // ===== 内部数据结构 =====

    public static class OreEntry {
        private final Item item;
        private final int weight;
        private final int minCount;
        private final int maxCount;
        private final long energyPerOperation;

        public OreEntry(Item item, int weight, int minCount, int maxCount, long energyPerOperation) {
            this.item = item;
            this.weight = weight;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.energyPerOperation = energyPerOperation;
        }

        public Item  getItem()               { return item; }
        public int   getWeight()             { return weight; }
        public int   getMinCount()           { return minCount; }
        public int   getMaxCount()           { return maxCount; }
        public long  getEnergyPerOperation() { return energyPerOperation; }
    }

    /** 用于 Gson 反序列化的中间数据结构 */
    private static class OreEntryData {
        String item;
        int weight = 10;
        int minCount = 1;
        int maxCount = 1;
        int energyPerOperation = 50_000;
    }
}
