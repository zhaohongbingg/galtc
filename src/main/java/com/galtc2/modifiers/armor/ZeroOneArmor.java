package com.galtc2.modifiers.armor;

import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 零一（护甲侧）：延长受击后的无敌帧。
 * <p>
 * 工具侧 {@code galtc2:eightstrike} 是「命中后清零目标无敌帧」，这条是反过来的自保：
 * 挨打后把自身的 {@code invulnerableTime} 顶到比原版 20 tick 更长，挡掉连续多段伤害。
 * <p>
 * <b>为什么不能在 {@code ON_ATTACKED} 里改</b>：{@code ON_ATTACKED} 触发时无敌帧还没结算，
 * 原版随后会把自己的 20 tick 写回去，我们设的值被覆盖 → 看起来「完全没生效」。
 * 所以改在 {@link LivingDamageEvent}（伤害已结算、原版已写好无敌帧）里再顶一次。
 */
public class ZeroOneArmor extends BaseModifiers {

    /** 原版受击后固定的无敌时间 */
    private static final int VANILLA_INVULNERABLE = 20;
    /** 每级额外延长 */
    private static final int BONUS_TICKS_PER_LEVEL = 10;

    public ZeroOneArmor() {
    }

    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public void modifierOnAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                                   EquipmentSlot slotType, net.minecraft.world.damagesource.DamageSource source,
                                   float amount, boolean isDirectDamage) {
        // 这里只记录「刚刚挨打了」，真正的无敌帧延长在 LivingDamageEvent 里做
        LivingEntity holder = context.getEntity();
        if (holder == null || holder.level().isClientSide()) {
            return;
        }
        holder.getPersistentData().putLong("galtc2:zeroone_armor_hit",
                holder.level().getGameTime());
    }

    @Override
    public void LivingDamageEvent(LivingDamageEvent event) {
        LivingEntity holder = event.getEntity();
        if (holder == null || holder.level().isClientSide()) {
            return;
        }
        long now = holder.level().getGameTime();
        long hit = holder.getPersistentData().getLong("galtc2:zeroone_armor_hit");
        if (hit <= 0L || now - hit > 5L) {
            // 不是刚被打（这条事件也可能来自其它伤害源）
            return;
        }
        int expected = VANILLA_INVULNERABLE + BONUS_TICKS_PER_LEVEL;
        if (holder.invulnerableTime < expected) {
            holder.invulnerableTime = expected;
        }
    }
}
