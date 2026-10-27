package com.galtc2.compat.aetherbedrock;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * 天境基石生物重制(Aether Bedrock Mobs Reborn) 软联动入口。
 * <p>
 * 注册方式和 J村 / 迎战 / 祸乱鬼魅 / 遗境守望 / 车万女仆 完全一致：编译期用 curse.maven 坐标
 * 引用它的物品 id 之外不引用任何类，运行期只靠 {@link ModList#isLoaded(String)} 判断，
 * 未安装时 galtc2 照常运行，联动强化与配方全部跳过。
 * <p>
 * <b>为什么只做强化、不做材料</b>：这个模组是「5 Boss 地牢包」而不是资源模组 ——
 * 它注册的是 5 把 Boss 专属武器（晶岩剑 / 地牢匕首 / 柴薪长矛 / 盐之器 / 灯灵剑）、
 * 钥匙、刷怪器、结构方块，唯一像资源的东西是 晶岩(crystalite) 与它的烧炼产物 晶岩尘
 * （尘在原模组里目前没有任何配方消费）。把成品武器熔成材料值会和「打完 Boss 拿奖励」的
 * 正反馈打架，而把战利品做成强化配方原料正好接上「打 Boss → 解锁匠魂强化」这条循环。
 * <p>
 * <b>命名空间优先级（两个包里有一堆同名方块，必须按这个顺序选）</b>：
 * <ol>
 *   <li>{@value #BMR_MOD_ID} —— 首选。Boss 包本体，注册了全部 5 把武器、城市之钥/终末之钥、
 *       晶岩/晶岩尘、蟹肉、晶岩建材、天境灯、四座刷怪器。凡是「打 Boss 掉的战利品」都在这里。</li>
 *   <li>{@value #DRAGON_MOD_ID} —— 只在 Boss 包没有对应物品时才用：天境龙笛、阴影护甲四件、
 *       极光石(aurorastone)、完整金光(aureate)套装。它是 Boss 包的强制前置。</li>
 *   <li>{@code aether}（天境本体）—— 不要用，避免和本体版本冲突。</li>
 * </ol>
 * 两包各自注册了一套同名方块（crystalitestone / saltstone* / labyrinthwall_* / mosaics 等），
 * <b>选定一包就只用那一包</b>；刷怪蛋两边也各有一份，配方里不要用刷怪蛋。
 * 本批 3 个强化刻意只引用 Boss 包，这样即使没装 aether_dragon 的额外内容也不会缺料。
 * <p>
 * 模组信息：CurseForge 项目 1739137 / 文件 9306972 =
 * aether_bedrock_mobs_reborn-1.20.1-forge-1.9.1.jar，modid {@value #BMR_MOD_ID}，
 * 许可 CC0-1.0；强制前置 geckolib ≥4.8.4 与 {@value #DRAGON_MOD_ID} ≥1.4.1。
 * 地牢只生成在 {@code aether:skyroot_*} 群系，完整体验还需要天境本体。
 */
public final class AetherBedrockCompat {

    /** 天境基石生物重制 主模组 ID（命名空间优先级第 1 位） */
    public static final String BMR_MOD_ID = "aether_bedrock_mobs_reborn";

    /** 天境巨龙 主模组 ID（Boss 包的强制前置；阴影护甲/龙笛/极光石在它那边） */
    public static final String DRAGON_MOD_ID = "aether_dragon";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean active = false;

    private AetherBedrockCompat() {
    }

    /** 运行环境是否安装了这个 Boss 包 */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(BMR_MOD_ID);
    }

    /** 联动是否已经启用（已安装且 init 执行成功） */
    public static boolean isActive() {
        return active;
    }

    /** 在模组构造阶段调用。未安装时直接返回，不加载任何联动类。 */
    public static void init(IEventBus modEventBus) {
        if (!isLoaded()) {
            LOGGER.info("[galtc2] 未检测到 天境基石生物重制({})，跳过联动内容。", BMR_MOD_ID);
            return;
        }
        active = true;

        // 3 个联动强化（全部只吃 Boss 包自己的战利品，特效直接调用原模组的技能实体）：
        //   娜迦震地   —— 地牢匕首（天境娜迦掉落）：命中概率召来娜迦的巨石（预警环 + 落石 + 震地碎石）
        //   晶笼       —— 晶岩拱心石 + 晶岩（晶岩魔像/仆从掉落）：受击时在攻击者脚下升起晶笼
        //   灯灵天剑   —— 灯灵剑 + 晶岩尘 + 天境灯（灯壶之灵掉落）：右键召来灯壶之灵的大剑
        // 表现与伤害都走原模组的 SpiritSkillEntity / NagaSkillEntity / GolemSkillEntity，
        // 见 {@link AetherBedrockEffects}；语言键见 assets/galtc2/lang/{zh_cn,en_us}.json。
        AetherBedrockModifiers.MODIFIERS.register(modEventBus);

        // 晶刺冲撞的触发（高速落地判定 + 地刺生成）挂在玩家 tick 上，
        // 不依赖护甲物品 tick，见 {@link GolemSpikeChargeHandler}。
        MinecraftForge.EVENT_BUS.addListener(GolemSpikeChargeHandler::onPlayerTick);

        LOGGER.info("[galtc2] 已启用 天境基石生物重制(Aether Bedrock Mobs Reborn) 联动：娜迦震地 / 晶笼 / 晶刺冲撞 / 灯灵天剑。");
    }
}
