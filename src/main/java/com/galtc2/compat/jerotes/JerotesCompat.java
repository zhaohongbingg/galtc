package com.galtc2.compat.jerotes;

import com.galtc2.modifiers.SayaModifier;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * J村(Jerotes Village) 软联动入口。
 * <p>
 * 这是"联动而非依赖"：编译期用 {@code compileOnly} 引用 J村 API，运行期通过
 * {@link ModList#isLoaded(String)} 判断。未安装 J村 时不会触碰任何 J村 类，
 * galtc2 照常运行。
 * <p>
 * 注意：所有引用 J村 类型的类（{@link JerotesModifiers}、{@link JerotesEvents}、
 * 各个特性实现）都只能从 {@link #init(IEventBus)} 内部按需加载，禁止添加
 * {@code @Mod.EventBusSubscriber} 自动注册。
 */
public final class JerotesCompat {

    /** J村 主模组 ID */
    public static final String JEROTES_MOD_ID = "jerotesvillage";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean active = false;

    private JerotesCompat() {
    }

    /** 运行环境是否安装了 J村 */
    public static boolean isJerotesLoaded() {
        return ModList.get().isLoaded(JEROTES_MOD_ID);
    }

    /** 联动是否已经启用（已安装 J村 且 init 执行成功） */
    public static boolean isActive() {
        return active;
    }

    /**
     * 在模组构造阶段调用。未安装 J村 时直接返回，不加载任何 J村 相关类。
     */
    public static void init(IEventBus modEventBus) {
        if (!isJerotesLoaded()) {
            LOGGER.info("[galtc2] 未检测到 J村({})，跳过联动内容。", JEROTES_MOD_ID);
            return;
        }
        active = true;

        // 注册 J村主题的匠魂强化/特性
        JerotesModifiers.MODIFIERS.register(modEventBus);
        // 注册 J村 联动材料的流体（未装 J村 时不注册，避免孤儿流体）
        JerotesFluids.register(modEventBus);
        // 注册事件侧效果（火系亲和输出等）
        MinecraftForge.EVENT_BUS.register(JerotesEvents.class);
        // 沙耶特性：攻击后召唤 J村 触手
        SayaModifier.setSummoner(new JerotesTentacleSummoner());
        // 勺景特性：攻击/受击时呼唤 J村 暗乱
        com.galtc2.modifiers.ShoujingModifier.setSummoner(new JerotesDarisorderSummoner());
        // 腐姬特性：命中时降下 J村 不洁血雨
        com.galtc2.modifiers.EuthanasiaModifier.setBloodRain(new JerotesBloodRainCaster());
        // 清枫特性：命中时召唤 J村 疫病虫群
        com.galtc2.modifiers.QingfengModifier.setSwarm(new JerotesInsectPlagueCaster());

        LOGGER.info("[galtc2] 已启用 J村 联动：魔法刻印 / 火系亲和 / 沙耶触手 / 守境达里斯秩序 / 腐姬血雨 / 清枫虫群 / 联动流体。");
    }
}
