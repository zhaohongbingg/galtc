package com.galtc2.combat;

import com.galtc2.Galtc2;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 破限伤害的兜底监听器：把被其他 mod 限制、削减掉的伤害拉回
 * {@link DamageBreaker} 登记的期望值。
 *
 * <p>两个事件都挂 {@link EventPriority#LOWEST}。LOWEST 是同一次伤害流程里
 * 我们能插进去的最后一个位置 —— 无论其他 mod 用什么优先级注册监听器，
 * 它们都已经执行完，我们看到的数值就是它们最终敲定的数值。
 *
 * <p>注意这里的语义是「保底」而不是「放大」：只在这条攻击本应更疼、
 * 却被限伤/减伤压下去时补回差额，已经超出期望值的伤害不会被改动。
 * 所以它不会叠加增伤，也不会破坏没有限伤机制时的原版数值。
 */
@Mod.EventBusSubscriber(modid = Galtc2.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DamageBreakerEvents {

    private DamageBreakerEvents() {}

    /**
     * 第一段：伤害刚进入实体伤害流程时的值（护甲尚未结算）。
     *
     * <p>在这一步兜底是为了防御「某个 mod 把 amount 压到 0」的情况 ——
     * {@code actuallyHurt} 里有 {@code if (pAmount <= 0) return;}，
     * 一旦被压到 0，后面的 {@link LivingDamageEvent} 根本不会触发，我们就彻底没机会了。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled()) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        float expected = DamageBreaker.peek(target);
        if (expected > 0.0F && event.getAmount() < expected) {
            event.setAmount(expected);
        }
    }

    /**
     * 第二段：护甲 / 抗性 / 吸收都算完、即将写入血量前的最终值。
     *
     * <p>这是整个伤害流程的最后一个可干预点：在这里 {@code setAmount} 之后
     * 数值会直接进入 {@code setHealth(getHealth() - amount)}，
     * 不再经过任何减伤计算。任何在 {@code hurt()} 覆写里做的封顶、
     * 或在事件层做的减伤，到这一步都已经被越过。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.isCanceled()) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        float expected = DamageBreaker.poll(target);
        if (expected > 0.0F && event.getAmount() < expected) {
            event.setAmount(expected);
        }
    }
}
