package com.galtc2.content.effect;

import com.galtc2.Register.galtc2Effects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class StunEventHandler {

    // 怪物：LivingTickEvent 在 AI 处理前触发
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        // 玩家走 PlayerTickEvent，这里跳过
        if (entity instanceof Player) return;
        if (!entity.hasEffect(galtc2Effects.STUN.get())) return;

        applyStun(entity);
    }

    // 玩家：PlayerTickEvent.END 在移动输入处理后触发
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (!player.hasEffect(galtc2Effects.STUN.get())) return;

        applyStun(player);
    }

    private static void applyStun(LivingEntity entity) {
        // 定身：XZ归零，Y仅允许下落
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(0, Math.min(motion.y, 0), 0);

        // 清除移动输入
        entity.xxa = 0;
        entity.yya = 0;
        entity.zza = 0;
        entity.setSprinting(false);

        // 怪物额外处理
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.getMoveControl().setWantedPosition(entity.getX(), entity.getY(), entity.getZ(), 0);
        }

        // 玩家额外处理
        if (entity instanceof Player player) {
            player.setShiftKeyDown(false);
            player.stopUsingItem();
        }
    }
}
