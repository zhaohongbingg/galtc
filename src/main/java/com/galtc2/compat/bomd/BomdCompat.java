package com.galtc2.compat.bomd;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * 祸乱鬼魅(Bosses of Mass Destruction, 缩写 BOMD) 软联动入口。
 * <p>
 * 注册方式和 J村({@link com.galtc2.compat.jerotes.JerotesCompat}) / 迎战
 * ({@link com.galtc2.compat.meetyourfight.MeetYourFightCompat}) 完全一致：
 * 编译期用 curse.maven 坐标引用 BOMD API，运行期只用 {@link ModList#isLoaded(String)}
 * 判断，未安装 BOMD 时不会触碰任何 BOMD 类，galtc2 照常运行。
 * <p>
 * 注意：以后所有引用 BOMD 类型的类（强化 / 材料 / 事件等）都只能从 {@link #init(IEventBus)}
 * 内部按需加载，禁止给它们加 {@code @Mod.EventBusSubscriber} 自动注册
 * ——那样未装 BOMD 时 Forge 也会去加载这些类，直接 NoClassDefFoundError。
 * <p>
 * 模组信息：CurseForge 项目 941573 / 文件 5067728 =
 * BOMD-Forge-1.20.1-1.1.2.jar，modid {@code bosses_of_mass_destruction}，
 * 作者 CerbonXD（原版是 Barribob 为 Fabric 写的），协议 LGPL-3.0。
 * 它自己的强制前置是 {@code cerbons_api} / {@code geckolib} / {@code cloth_config}。
 */
public final class BomdCompat {

    /** 祸乱鬼魅 主模组 ID */
    public static final String BOMD_MOD_ID = "bosses_of_mass_destruction";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean active = false;

    private BomdCompat() {
    }

    /** 运行环境是否安装了祸乱鬼魅 */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(BOMD_MOD_ID);
    }

    /** 联动是否已经启用（已安装 BOMD 且 init 执行成功） */
    public static boolean isActive() {
        return active;
    }

    /**
     * 在模组构造阶段调用。未安装祸乱鬼魅 时直接返回，不加载任何 BOMD 相关类。
     */
    public static void init(IEventBus modEventBus) {
        if (!isLoaded()) {
            LOGGER.info("[galtc2] 未检测到 祸乱鬼魅({})，跳过联动内容。", BOMD_MOD_ID);
            return;
        }
        active = true;

        // 联动强化/特性（机制一律复用 BOMD 自己的实现，别再造轮子）：
        //   魂人 —— 材料特性（对亡灵与暗夜巫妖 +25% 伤害 / 受其伤害 -25%）
        //   星空下的记忆 —— 右键随机射出坠星(comet) 或霜星(magic_missile)，参数照 CometAction
        //   永恒少女 —— 命中后围目标撒一圈"虚空之花的裂刺"(void_blossom.Spikes)，会扎人
        //   宝石少女 —— 命中后围目标撒一圈"黑曜巨石的地刺"(obsidilith.RiftBurst)，会扎人 + 减速
        //   祐天寺美汐 —— 按住右键射出激光，长度/判定/伤害源/粒子/音效全走 BOMD 的公开资源
        // 对应材料/配方 JSON 见 data/galtc2/tinkering/materials/** 与 data/galtc2/recipes/**
        // （同样带 bosses_of_mass_destruction 的 mod_loaded 条件）。
        BomdModifiers.MODIFIERS.register(modEventBus);
        // 联动方块：galtc2:obsidian_spike。
        // 它已经不再被「宝石少女」使用（那边现在直接用 BOMD 的 RiftBurst：
        // 粒子 + 延迟伤害，而不是真方块），保留注册只是为了不让老存档里的该方块变成"缺失方块"。
        BomdBlocks.BLOCKS.register(modEventBus);

        LOGGER.info("[galtc2] 已启用 祸乱鬼魅(Bosses of Mass Destruction) 联动："
                + "魂人 / 星空下的记忆 / 永恒少女 / 宝石少女 / 祐天寺美汐。");
    }
}
