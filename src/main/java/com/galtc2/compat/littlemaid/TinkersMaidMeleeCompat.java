package com.galtc2.compat.littlemaid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 让女仆用匠魂近战武器攻击时走匠魂自己的攻击流程，从而触发**武器特性**（Fiery、Lacerating、Decay…）。
 * <p>
 * <b>问题</b>：女仆能识别匠魂近战武器（女仆按 {@code ATTACK_DAMAGE} 属性判断），伤害面板也对；
 * 但匠魂的近战特性入口是 {@code ModifiableItem#onLeftClickEntity(ItemStack, Player, Entity)}
 * —— 只有玩家攻击才会走，女仆（非玩家）攻击时特性全都不触发。
 * <p>
 * <b>做法</b>：监听 {@link LivingHurtEvent}，当「伤害来源是女仆、主手是匠魂近战工具」时：
 * 取消这次原版伤害，改调匠魂公开的 {@link ToolAttackUtil#attackEntity}（LivingEntity 重载），
 * 由匠魂重新结算伤害并触发全部近战钩子。用重入标记避免 {@code attackEntity -> hurt -> 事件}
 * 再次进来形成死循环；匠魂弓/弩（{@link ModifiableLauncherItem}）不在此处理。
 * <p>
 * 该类只在「装了车万女仆 + 匠魂」时由 {@link LittleMaidCompat#init} 注册监听，未装时不会加载。
 */
public final class TinkersMaidMeleeCompat {

    /** 重入标记：attackEntity 内部再次触发 LivingHurtEvent 时不再接管 */
    private static boolean attacking = false;

    private TinkersMaidMeleeCompat() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(TinkersMaidMeleeCompat::onLivingHurt);
    }

    private static void onLivingHurt(LivingHurtEvent event) {
        if (attacking) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof EntityMaid maid)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == maid) {
            return;
        }
        ItemStack stack = maid.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof ModifiableItem)) {
            return;
        }
        // 弓/弩属于远程武器，交给匠魂弓术任务，不在这里接管
        if (stack.getItem() instanceof ModifiableLauncherItem) {
            return;
        }
        IToolStackView tool = ToolStack.from(stack);
        if (tool.isBroken()) {
            return;
        }
        attacking = true;
        boolean replaced = false;
        try {
            // LivingEntity 重载：会按匠魂自己的流程结算伤害并触发全部近战特性
            replaced = ToolAttackUtil.attackEntity(tool, maid, InteractionHand.MAIN_HAND, target,
                    ToolAttackUtil.NO_COOLDOWN, false);
        } catch (Throwable ignored) {
            // 匠魂处理失败时保留原版伤害
        } finally {
            attacking = false;
        }
        // 只有匠魂真的接管并结算了伤害才取消原版伤害；否则让原版伤害照常生效，
        // 避免出现「女仆完全打不出伤害、手臂动画却一直在挥」的情况
        if (replaced) {
            event.setCanceled(true);
        }
    }
}
