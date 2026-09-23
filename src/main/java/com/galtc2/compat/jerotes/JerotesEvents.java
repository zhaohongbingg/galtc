package com.galtc2.compat.jerotes;

import com.galtc2.modifiers.QingfengModifier;
import com.jerotes.jerotes.entity.Other.OtherSpell.InsectPlagueEntity;
import com.jerotes.jerotes.util.AttackFind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * J村联动的事件侧效果。
 * <p>
 * 目前有两块：
 * <ul>
 *   <li>火系亲和的输出侧：攻击者穿戴带火焰亲和特性(如 {@link FireApostleModifier})的护甲时，
 *       按件数提升其造成的火系伤害</li>
 *   <li>清枫虫群的攻击力：不改 J村 虫群的法术等级，只按原版节奏多咬几口，见 {@link #onLevelTick(TickEvent.LevelTickEvent)}</li>
 * </ul>
 * 仅在 J村 存在时由 {@link JerotesCompat} 注册。
 */
public final class JerotesEvents {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    /** 清枫虫群额外撕咬的间隔（tick）：与 J村 原版虫群「每 5 tick 咬一口」对齐 */
    private static final int BITE_INTERVAL = 5;

    private JerotesEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker.level().isClientSide() || !AttackFind.isFireDamage(event.getSource())) {
            return;
        }
        float bonus = 0.0f;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = attacker.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ToolStack tool = ToolStack.from(stack);
            for (ModifierEntry entry : tool.getModifierList()) {
                if (entry.getModifier() instanceof FireAffinityModifier fire) {
                    bonus += fire.fireDealtBonus() * entry.getLevel();
                }
            }
        }
        if (bonus > 0.0f) {
            event.setAmount(event.getAmount() * (1.0f + bonus));
        }
    }

    /**
     * 清枫：虫群的攻击力成长。
     * <p>
     * <b>刻意不修改虫群的法术等级</b>（逐次伤害口径与原版一致，避免和 J村 之后要做的玩家法术体系打架），
     * 而是按原版同样的 5 tick 节奏，对虫群范围内的目标再多调用几次虫群自己的
     * {@link InsectPlagueEntity#dealDamageTo(LivingEntity)}：该方法内部已经处理了归属、
     * 无敌、盟友、法术豁免等判定，我们只是让它"多咬几口"。
     * <p>
     * 额外次数由 {@link QingfengModifier#bites(int)} 决定，写在虫群实体的
     * {@link QingfengModifier#KEY_EXTRA_BITES} 上；没打过标记的虫群（J村 原版法术放的）不受影响。
     */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        if (event.level.getGameTime() % BITE_INTERVAL != 0 || !(event.level instanceof ServerLevel server)) {
            return;
        }
        for (Entity entity : server.getAllEntities()) {
            if (!(entity instanceof InsectPlagueEntity swarm)) {
                continue;
            }
            int extra = swarm.getPersistentData().getInt(QingfengModifier.KEY_EXTRA_BITES);
            if (extra <= 0) {
                continue;
            }
            for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, swarm.getBoundingBox())) {
                for (int i = 0; i < extra; i++) {
                    swarm.dealDamageTo(target);
                }
            }
        }
    }
}
