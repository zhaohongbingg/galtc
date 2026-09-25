package com.galtc2.jvt.event;

import com.jerotes.jerotes.forge.JerotesFactionEvent;
import com.jerotes.jerotesvillage.util.OtherEntityFactionFind;
import com.galtc2.jvt.Register.JvtModifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;

/**
 * 贵族强化效果：复用 J村 自带的阵营系统。
 * <p>
 * 复魂尸(及沉降阵营生物)走 J村 的 {@code EntityFactionFind.isFaction}，会 post {@link JerotesFactionEvent}。
 * 当判定对象是玩家、且玩家任意护甲槽带有 {@code jvt:noble_upgrade} 修饰符时，把它标记为友军。
 * 这样复魂尸重写的 {@code canAttack}/{@code isAlliedTo} 都会认定玩家是友军：
 * 既不主动索敌，被玩家攻击也不会反击，效果与 J村 贵族护甲一致。
 */
public class NobleFactionEvents {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @SubscribeEvent
    public static void onFactionCheck(JerotesFactionEvent event) {
        LivingEntity first = event.getLiving1();
        LivingEntity second = event.getLiving2();
        if (first == null || second == null) return;

        // 找出"玩家 + 沉降生物"的组合
        Player player;
        LivingEntity sediment;
        if (first instanceof Player p) {
            player = p;
            sediment = second;
        } else if (second instanceof Player p) {
            player = p;
            sediment = first;
        } else {
            return;
        }

        if (!OtherEntityFactionFind.isResurrecter(sediment.getType())) return;
        if (!wearsNobleUpgrade(player)) return;

        event.setFriend(true);
        event.setEnemy(false);
    }

    /** 玩家四个护甲槽任意一件匠魂护甲带贵族强化即生效 */
    private static boolean wearsNobleUpgrade(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (ModifierUtil.getModifierLevel(stack, JvtModifier.NOBLE_UPGRADE) > 0) {
                return true;
            }
        }
        return false;
    }
}
