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



        }


    }
    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }
}
