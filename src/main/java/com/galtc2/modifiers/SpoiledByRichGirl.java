package com.galtc2.modifiers;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 被富婆包养了 - 与村民交互时获得额外好感
 */
public class SpoiledByRichGirl extends Modifier implements EntityInteractionModifierHook {

    public SpoiledByRichGirl() {}

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ENTITY_INTERACT);
    }

    @Override
    public InteractionResult afterEntityUse(IToolStackView tool, ModifierEntry modifier, Player player, LivingEntity target, InteractionHand hand, InteractionSource source) {
        // 当与村民交互时触发
        if (target instanceof Villager villager && !player.level().isClientSide) {
            // 可以在这里添加额外逻辑
            // 例如：给予玩家更好的交易价格（需要自定义实现）
        }
        return InteractionResult.PASS;
    }
}
