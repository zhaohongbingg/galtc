package com.galtc2.compat.meetyourfight;

import com.galtc2.compat.meetyourfight.modifiers.RoseWraithModifier;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * 迎战(Meet Your Fight) 软联动入口。
 * <p>
 * 注册方式和 J村({@link com.galtc2.compat.jerotes.JerotesCompat}) 完全一致：
 * 编译期用 curse.maven 坐标引用迎战 API，运行期只用 {@link ModList#isLoaded(String)} 判断，
 * 未安装迎战 时不会触碰任何迎战类，galtc2 照常运行。
 * <p>
 * 注意：以后所有引用迎战类型的类（强化 / 材料 / 事件等）都只能从 {@link #init(IEventBus)}
 * 内部按需加载，禁止给它们加 {@code @Mod.EventBusSubscriber} 自动注册
 * ——那样未装迎战 时 Forge 也会去加载这些类，直接 NoClassDefFoundError。
 */
public final class MeetYourFightCompat {

    /** 迎战 主模组 ID */
    public static final String MEETYOURFIGHT_MOD_ID = "meetyourfight";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean active = false;

    private MeetYourFightCompat() {
    }

    /** 运行环境是否安装了迎战 */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MEETYOURFIGHT_MOD_ID);
    }

    /** 联动是否已经启用（已安装迎战 且 init 执行成功） */
    public static boolean isActive() {
        return active;
    }

    /**
     * 在模组构造阶段调用。未安装迎战 时直接返回，不加载任何迎战相关类。
     */
    public static void init(IEventBus modEventBus) {
        if (!isLoaded()) {
            LOGGER.info("[galtc2] 未检测到 迎战({})，跳过联动内容。", MEETYOURFIGHT_MOD_ID);
            return;
        }
        active = true;

        // 迎战 主题的匠魂特性(trait)：
        //   魂喰 / 斩月双刀 / 圣战士 / 亲人的羁绊 / 根性 / 藓牙牙刺 / 舞亚
        // 对应材料 JSON 见 data/galtc2/tinkering/materials/**（同样带 meetyourfight 的 mod_loaded 条件）。
        MeetYourFightModifiers.MODIFIERS.register(modEventBus);
        // 舞亚召唤出来的蔷薇怨灵需要一个服务端 tick 清理器（到点/失去目标就回收）
        RoseWraithModifier.registerEvents();

        LOGGER.info("[galtc2] 已启用 迎战(Meet Your Fight) 联动：魂喰 / 斩月双刀 / 圣战士 / 亲人的羁绊 / 根性 / 藓牙牙刺 / 舞亚。");
    }
}
