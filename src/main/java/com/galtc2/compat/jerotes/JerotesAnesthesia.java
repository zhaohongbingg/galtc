package com.galtc2.compat.jerotes;

import com.jerotes.jerotes.init.JerotesMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegistryObject;

/**
 * J村 麻醉（{@code jerotes:anesthetized}）的软调用入口。
 * <p>
 * 背景：{@code mountain_realm}（镇山金属）远程命中要按用户要求给目标挂「麻醉」，
 * 但麻醉效果是 J村 的、且 J村 是<b>软联动</b>（见 {@link JerotesCompat}：
 * 编译期 {@code implementation} 依赖，运行期不装也必须能跑）。
 * 如果让 {@code jvt/modifiers/MountainRealmModifier} 直接 {@code import JerotesMobEffects}，
 * 未装 J村 的整合包会在类加载/校验阶段炸 {@code NoClassDefFoundError}。
 * <p>
 * 所以所有对 J村 类型的引用都收在下面这个<b>嵌套类</b> {@link EffectBridge} 里：
 * <ul>
 *   <li>{@link #apply} 本体只碰 Minecraft API 与 {@link ModList}，未装 J村 时直接返回 {@code false}；</li>
 *   <li>{@code EffectBridge} 只有在「确认 J村 在装」之后才会被调用 ——
 *       JVM 到那一刻才去链接/校验它，因此未装 J村 时 J村 类型根本不会被解析，
 *       不会抛异常、也不会刷 ERROR（符合 F-029 卡片 §2-7）。</li>
 * </ul>
 * <b>签名是固定契约</b>：{@code jvt` 侧的 {@code MountainRealmModifier} 按
 * {@link #apply(Level, LivingEntity, int, int)} 调用，不要改参数顺序/类型。
 */
public final class JerotesAnesthesia {

    /**
     * J村 <b>API</b> 模组 id。
     * <p>注意区分两个 jar（已核对 {@code META-INF/mods.toml} 与 run 日志）：
     * <ul>
     *   <li>{@code jerotes}（JerotesWarehouse）= 提供 {@code com.jerotes.jerotes.*} 的那个库，
     *       {@code JerotesMobEffects} 就在它里面；本类要调用的正是它，所以守卫用它；</li>
     *   <li>{@code jerotesvillage}（JerotesVillage）= 村本体（{@link JerotesCompat#JEROTES_MOD_ID}），
     *       依赖上面的库；玩家侧「装没装 J村」通常看它。</li>
     * </ul>
     * 装村本体必然连带库，所以用 {@code jerotes} 守卫既精确又不会误判。
     */
    public static final String JEROTES_API_MOD_ID = "jerotes";

    private JerotesAnesthesia() {
    }

    /**
     * 给目标挂 J村 麻醉效果。
     *
     * @param level         结算所在维度（客户端直接返回 false，效果只在服务端挂）
     * @param target        被麻醉的目标
     * @param durationTicks 持续时间（tick）
     * @param amplifier     效果等级，{@code 0} = 麻醉 I（与 {@code ChainBindModifier} 口径一致，负数按 0 处理）
     * @return 真的挂上去了返回 {@code true}；未装 J村 / 参数非法 / 效果未注册 返回 {@code false}，<b>不抛异常</b>
     */
    public static boolean apply(Level level, LivingEntity target, int durationTicks, int amplifier) {
        if (level == null || level.isClientSide() || target == null || !target.isAlive()) {
            return false;
        }
        if (durationTicks <= 0) {
            return false;
        }
        // 软联动守卫：未装 J村 时到这里就结束，下面那个嵌套类不会被链接，J村 类型也就不会被解析
        if (ModList.get() == null || !ModList.get().isLoaded(JEROTES_API_MOD_ID)) {
            return false;
        }
        return EffectBridge.apply(target, durationTicks, amplifier);
    }

    /** 运行环境是否装了提供 J村 API 的那个模组（{@code jerotes}） */
    public static boolean isAvailable() {
        return ModList.get() != null && ModList.get().isLoaded(JEROTES_API_MOD_ID);
    }

    /**
     * 真正碰 J村 类型的地方（惰性链接的隔离层）。
     * <p>只有 {@link #apply} 的 ModList 守卫通过后才会进来；未装 J村 时本类不会被初始化。
     */
    private static final class EffectBridge {

        private EffectBridge() {
        }

        private static boolean apply(LivingEntity target, int durationTicks, int amplifier) {
            RegistryObject<MobEffect> anesthesia = JerotesMobEffects.ANESTHETIZED;
            // 防御：API 模组装了、但效果本身没注册（极端整合包）时静默退化，不 NPE
            if (!anesthesia.isPresent()) {
                return false;
            }
            // 与 ChainBindModifier 的写法对齐：ambient=false / visible=true / showIcon=true
            target.addEffect(new MobEffectInstance(anesthesia.get(), durationTicks,
                    Math.max(0, amplifier), false, true, true));
            return true;
        }
    }
}
