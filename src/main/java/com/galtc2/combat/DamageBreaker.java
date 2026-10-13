package com.galtc2.combat;

import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 破限（Breakthrough）核心：登记「这一下本该造成多少伤害」，
 * 由 {@link DamageBreakerEvents} 在伤害流程的最后一步把它兜回来。
 *
 * <p>为什么必须这样做 —— Forge 1.20.1 的 {@code LivingEntity#actuallyHurt} 顺序是：
 * <pre>
 *   LivingHurtEvent  ->  护甲  ->  抗性/保护  ->  吸收  ->  LivingDamageEvent  ->  setHealth()
 * </pre>
 * 而绝大多数 mod 的「单次伤害上限」是在 {@code hurt(DamageSource, float)} 覆写里、
 * 也就是整套事件流程<b>之前</b>就把 amount 截断掉的（本整合包的 GalBoss 就是这样）。
 * 等事件触发时，amount 只剩残值，对它乘任何倍率都还原不出原始伤害 ——
 * 除非我们事前就知道「本来应该是多少」。
 *
 * <p>所以在攻击发起侧登记期望值，再由事件在本 tick 内兜底，
 * 是唯一既不需要 Mixin 注入、又能同时覆盖
 * 「hurt 覆写里封顶」「任意优先级的减伤监听器」的做法。
 *
 * <p>登记只在<b>当前游戏刻</b>内有效：伤害的整套流程（hurt -> 事件 -> setHealth）
 * 是同一个 tick 内同步跑完的，放宽窗口只会误伤同一刻打上来的其他攻击。
 */
public final class DamageBreaker {

    private record Expectation(float amount, long tick) {}

    /** 用 WeakHashMap：目标实体被回收后登记项自动消失，不会长期占内存。 */
    private static final Map<LivingEntity, Expectation> EXPECTATIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private DamageBreaker() {}

    /**
     * 登记：即将打在 {@code target} 身上的这一下，本应造成 {@code amount} 点伤害。
     * 必须在调用 {@code target.hurt(source, amount)} <b>之前</b>调用。
     *
     * <p>如果这次攻击最终被原版流程拒绝（目标免疫、阶段无敌等），登记项会在本刻结束后
     * 自然失效，不会残留到下一次伤害上。
     */
    public static void expect(LivingEntity target, float amount) {
        if (target == null || target.level().isClientSide()) {
            return;
        }
        if (!Float.isFinite(amount) || amount <= 0.0F) {
            return;
        }
        EXPECTATIONS.put(target, new Expectation(amount, target.level().getGameTime()));
    }

    /**
     * 取出本刻登记的期望值，取完即清，避免同一刻的两段事件
     * （{@code LivingHurtEvent} / {@code LivingDamageEvent}）之外的后续伤害被重复抬高。
     *
     * @return 期望伤害；没有登记、已过期或超出本刻时返回 {@code 0}
     */
    public static float poll(LivingEntity target) {
        if (target == null) {
            return 0.0F;
        }
        Expectation expectation = EXPECTATIONS.remove(target);
        if (expectation == null) {
            return 0.0F;
        }
        return expectation.tick() == target.level().getGameTime() ? expectation.amount() : 0.0F;
    }

    /**
     * 只看不取。给 {@code LivingHurtEvent} 用：那一步还不是最终值，
     * 真正的兜底与清理交给后面的 {@code LivingDamageEvent}。
     */
    public static float peek(LivingEntity target) {
        if (target == null) {
            return 0.0F;
        }
        Expectation expectation = EXPECTATIONS.get(target);
        if (expectation == null) {
            return 0.0F;
        }
        return expectation.tick() == target.level().getGameTime() ? expectation.amount() : 0.0F;
    }

    /** 显式丢弃登记（例如攻击在发起后被判定不成立）。 */
    public static void discard(LivingEntity target) {
        if (target != null) {
            EXPECTATIONS.remove(target);
        }
    }
}
