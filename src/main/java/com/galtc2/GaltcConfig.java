package com.galtc2;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.shared.TinkerMaterials;

import java.util.List;
import java.util.function.Supplier;



@Mod.EventBusSubscriber(modid = Galtc2.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GaltcConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    // ===== Static Init =====
    static {
        Pair<Common, ForgeConfigSpec> commonPair =
                new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = commonPair.getLeft();
        COMMON_SPEC = commonPair.getRight();

        Pair<Client, ForgeConfigSpec> clientPair =
                new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();

        Pair<Server, ForgeConfigSpec> serverPair =
                new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();
    }

    // ===== Inner Classes =====


    public static class Client {
        public Client(ForgeConfigSpec.Builder builder) {
        }
    }

    public static class Server {
        public Server(ForgeConfigSpec.Builder builder) {
        }
    }



    public static class Common {

        public final ForgeConfigSpec.IntValue HIT_THRESHOLD;
        public final ForgeConfigSpec.DoubleValue ATTACK_THRESHOLD;

        public final ForgeConfigSpec.DoubleValue IMMUNE_TICKS;
        public final ForgeConfigSpec.DoubleValue Azarashi_ATTACK_THRESHOLD;

        // 所长(Shochou) 财迷特性配置
        public final ForgeConfigSpec.ConfigValue<String> SHOCHOU_MONEY_ITEM;
        public final ForgeConfigSpec.DoubleValue SHOCHOU_DROP_CHANCE;
        public final ForgeConfigSpec.IntValue SHOCHOU_DROP_AMOUNT;

        // 心之壁配置
        public final ForgeConfigSpec.IntValue HEARTWALL_SHIELD_PER_LEVEL;

        // 星空列车配置
        public final ForgeConfigSpec.IntValue STARRY_TRAVEL_CHUNKS_PER_DAMAGE;
        public final ForgeConfigSpec.IntValue STARRY_TRAVEL_CHUNKS_PER_ARMOR;

        public Common(ForgeConfigSpec.Builder builder) {

            this.HIT_THRESHOLD = builder
                    .comment("受多少次攻击增加护甲")
                    .defineInRange("hitthreshold", 50, 10, 1000);
            this.ATTACK_THRESHOLD = builder
                    .comment("低于多少攻击力，夜羊生效")
                    .comment("计算公式为：damage*(ATTACK_THRESHOLD-damage)")
                    .defineInRange("attackthreshold", 50.0, 18, 100);
            this.IMMUNE_TICKS=builder
                    .comment("受到攻击后的免疫时间,按tick算，1s=20tick")
                    .defineInRange("immuneticks", 600.0, 100, 10000);
            this.Azarashi_ATTACK_THRESHOLD= builder
                    .comment("海豹翻倍的最小攻击力")
                    .defineInRange("azarashi_attackthreshold", 100.0, 50.0,Double.MAX_VALUE);

            // 所长(Shochou) 财迷特性
            this.SHOCHOU_MONEY_ITEM = builder
                    .comment("财迷特性：掉落的物品ID，默认为绿宝石(minecraft:emerald)")
                    .define("shochouMoneyItem", "minecraft:emerald");
            this.SHOCHOU_DROP_CHANCE = builder
                    .comment("财迷特性：每次攻击掉落概率，0.8表示80%")
                    .defineInRange("shochouDropChance", 0.8, 0.01, 1.0);
            this.SHOCHOU_DROP_AMOUNT = builder
                    .comment("财迷特性：单次掉落数量上限（在1~上限之间随机掉落）")
                    .defineInRange("shochouDropAmount", 10, 1, 10);

            // 心之壁配置
            this.HEARTWALL_SHIELD_PER_LEVEL = builder
                    .comment("心之壁：每层提供的护盾值")
                    .defineInRange("heartwallShieldPerLevel", 20, 1, 1000);

            // 星空列车配置
            this.STARRY_TRAVEL_CHUNKS_PER_DAMAGE = builder
                    .comment("星空列车：每多少区块增加1点伤害")
                    .defineInRange("starryTravelChunksPerDamage", 5, 1, 100);
            this.STARRY_TRAVEL_CHUNKS_PER_ARMOR = builder
                    .comment("星空列车：每多少区块增加1点护甲值和韧性")
                    .defineInRange("starryTravelChunksPerArmor", 5, 1, 100);

        }


    }
    /**
     * @param context 由 {@link Galtc2} 构造函数里注入的 mod 加载上下文
     *                （{@code ModLoadingContext.get()} 已被标记待删除，改成用注入的实例）
     */
    public static void init(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        context.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }
}
