package com.galtc2.compat.relicward;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * 遗境守望(Relic Ward) 软联动入口。
 * <p>
 * 注册方式和 J村({@link com.galtc2.compat.jerotes.JerotesCompat}) / 迎战
 * ({@link com.galtc2.compat.meetyourfight.MeetYourFightCompat}) / 祸乱鬼魅
 * ({@link com.galtc2.compat.bomd.BomdCompat}) 完全一致：
 * 编译期用 curse.maven 坐标引用遗境守望 API，运行期只用 {@link ModList#isLoaded(String)}
 * 判断，未安装遗境守望 时不会触碰任何遗境守望类，galtc2 照常运行。
 * <p>
 * 注意：以后所有引用遗境守望类型的类（强化 / 材料 / 事件等）都只能从 {@link #init(IEventBus)}
 * 内部按需加载，禁止给它们加 {@code @Mod.EventBusSubscriber} 自动注册
 * ——那样未装遗境守望 时 Forge 也会去加载这些类，直接 NoClassDefFoundError。
 * <p>
 * 模组信息：CurseForge 项目 1694157 / 文件 8871420 = relicward-1.0.0.jar，
 * modid {@code relicward}，包名 {@code cn.suiyi.relicward}，作者 taisuiyileba（太岁一乐吧），
 * 协议 MIT。它自己的强制前置是 {@code curios}（可选 Jade），<b>不依赖灾变与 GeckoLib</b>。
 */
public final class RelicWardCompat {

    /** 遗境守望 主模组 ID */
    public static final String RELICWARD_MOD_ID = "relicward";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean active = false;

    private RelicWardCompat() {
    }

    /** 运行环境是否安装了遗境守望 */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(RELICWARD_MOD_ID);
    }

    /** 联动是否已经启用（已安装遗境守望 且 init 执行成功） */
    public static boolean isActive() {
        return active;
    }

    /**
     * 在模组构造阶段调用。未安装遗境守望 时直接返回，不加载任何遗境守望相关类。
     */
    public static void init(IEventBus modEventBus) {
        if (!isLoaded()) {
            LOGGER.info("[galtc2] 未检测到 遗境守望({})，跳过联动内容。", RELICWARD_MOD_ID);
            return;
        }
        active = true;

        // 遗境守望 主题的匠魂强化/特性（机制一律复用遗境守望 自己的实现，别再造轮子）：
        //   回响震击 —— 右键蓄力后松手落锤，伤害 = 面板伤害 × 蓄力秒数 × 等级，
        //               判定/表现照回响钟锤的震击（CombatMath.cone + MaulPulse + ArmorFracture）
        //   钟甲     —— 每级 +5% 抗击退，单次 6 点以上伤害减免 30%
        // 这两个挂在 钟铸铜板 材料的 traits 上（data/galtc2/tinkering/materials/traits/resonant_plate.json）。
        // 另有 裂甲 / 钟波 / 巡庭铜傀 三个特性写好了但暂时没被任何材料用到，注册已在
        // RelicWardModifiers 里注释掉（源码保留）。
        // 对应语言键见 assets/galtc2/lang/{zh_cn,en_us}.json 的 modifier.galtc2.<id>*。
        RelicWardModifiers.MODIFIERS.register(modEventBus);

        LOGGER.info("[galtc2] 已启用 遗境守望(Relic Ward) 联动：回响震击 / 钟甲（钟铸铜板）。");
    }
}
