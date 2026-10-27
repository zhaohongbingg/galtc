package com.galtc2.compat.aetherbedrock.modifiers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * 晶刺冲撞 (golem_spike_charge) —— 天境基石生物重制 联动强化（护甲侧，占用升级槽）。
 * <p>
 * 模仿水晶石傀儡的「冲撞」：玩家以超过 7 格/秒的水平速度<b>接触地面</b>、且<b>不在载具/坐骑上</b>时，
 * 沿速度反方向在身后踏出一串（7 根）魔像的晶刺（{@code GolemSkillEntity.SPIKE}：
 * 破土伤害 + 碎晶特效 + 晶刺音效，全部由原模组演出）。冷却 10 秒。
 * <p>
 * 本类只负责注册与 tooltip；触发逻辑（速度峰值、落地判定、冷却、生成地刺）在
 * {@link com.galtc2.compat.aetherbedrock.GolemSpikeChargeHandler} 里，
 * 挂在服务端玩家 tick 上，避免依赖护甲物品 tick 而漏触发。
 */
public class GolemSpikeChargeModifier extends Modifier implements TooltipModifierHook {

    public GolemSpikeChargeModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player,
                           List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable("modifier.galtc2.golem_spike_charge.tip")
                .withStyle(ChatFormatting.GRAY));
    }
}
