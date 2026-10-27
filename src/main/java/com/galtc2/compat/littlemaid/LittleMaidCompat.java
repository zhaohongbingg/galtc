package com.galtc2.compat.littlemaid;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * 车万女仆(Touhou Little Maid) 软联动入口。
 * <p>
 * 注册方式和 J村 / 迎战 / 祸乱鬼魅 / 遗境守望 完全一致：编译期用 curse.maven 坐标引用
 * 车万女仆 的 API（见 {@code build.gradle}），运行期只用 {@link ModList#isLoaded(String)} 判断，
 * 未安装车万女仆 时不会触碰任何车万女仆 的类，galtc2 照常运行。
 * <p>
 * 注意：所有引用 {@code EntityMaid} 的类（{@link LittleMaidModifiers} 及其下面的特性）都只能从
 * {@link #init(IEventBus)} 内部按需加载，<b>禁止</b>给它们加 {@code @Mod.EventBusSubscriber}
 * 自动注册 —— 那样未装车万女仆 时 Forge 也会去加载这些类，直接 NoClassDefFoundError。
 */
public final class LittleMaidCompat {

    /** 车万女仆 主模组 ID */
    public static final String LITTLE_MAID_MOD_ID = "touhou_little_maid";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean active = false;

    private LittleMaidCompat() {
    }

    /** 运行环境是否安装了车万女仆 */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(LITTLE_MAID_MOD_ID);
    }

    /** 联动是否已经启用（已安装车万女仆 且 init 执行成功） */
    public static boolean isActive() {
        return active;
    }

    /**
     * 在模组构造阶段调用。未安装车万女仆 时直接返回，不加载任何车万女仆 相关类。
     */
    public static void init(IEventBus modEventBus) {
        if (!isLoaded()) {
            LOGGER.info("[galtc2] 未检测到 车万女仆({})，跳过联动内容。", LITTLE_MAID_MOD_ID);
            return;
        }
        active = true;

        // 「酒狐」= 玩家的车万女仆女仆。三个特性都围绕她：
        //   日久生情 —— 工具侧：按她的好感度给工具补基础面板（耐久 / 攻击 / 挖掘速度）
        //   挚爱     —— 远程侧：箭矢命中后让目标 5 秒内优先索敌「除了玩家与酒狐以外」的生物
        //   同心     —— 护甲侧：玩家穿 → 酒狐 80% 伤害转给玩家；酒狐穿 → 抗性提升 II + 生命恢复 III
        // 语言键见 assets/galtc2/lang/{zh_cn,en_us}.json 的 modifier.galtc2.love_grows / true_love / one_heart。
        LittleMaidModifiers.MODIFIERS.register(modEventBus);

        // 酒狐干活时往主人背包里产 酒狐锭（纯服务端，见 JiuHuWorkProduction）
        JiuHuWorkProduction.register();

        // 匠魂近战武器：让女仆攻击也走匠魂的 ToolAttackUtil，触发武器特性（见 TinkersMaidMeleeCompat）
        if (ModList.get().isLoaded("tconstruct")) {
            TinkersMaidMeleeCompat.register();
        }

        // 匠魂弓术等工作模式走车万女仆 官方扩展接口：
        // {@link Galtc2MaidExtension}（@LittleMaidExtension + ILittleMaid#addMaidTask）。

        LOGGER.info("[galtc2] 已启用 车万女仆 联动：日久生情 / 挚爱 / 同心 / 酒狐锭产出 / 匠魂武器兼容。");
    }
}
