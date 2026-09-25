package com.galtc2.jvt.modifiers;

import com.galtc2.compat.jerotes.SpellCastHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 蓄力施法能力基类：长按右键蓄力(≥10 tick)后松手，通过 {@link SpellCastHelper} 施放指定法术。
 * 占用能力槽，且不兼容其他武器能力强化。
 */
public abstract class ChargedSpellAbilityModifier extends Modifier implements GeneralInteractionModifierHook, ValidateModifierHook {

    private static final int USE_DURATION = 72000;
    private static final int MIN_CHARGE = 10;

    /** 要施放的法术 ID */
    protected abstract String spellId();

    /** 法术等级 */
    protected int spellLevel() {
        return 3;
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.VALIDATE);
    }

    @Override
    public Component validate(IToolStackView tool, ModifierEntry modifier) {
        return WeaponEnhancements.validateExclusive(tool, modifier);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK && !tool.isBroken()) {
            GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAnim.BLOCK;
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        tryCast(tool, entity, USE_DURATION - timeLeft);
    }

    @Override
    public void onFinishUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity) {
        tryCast(tool, entity, USE_DURATION);
    }

    private void tryCast(IToolStackView tool, LivingEntity entity, int heldTicks) {
        if (heldTicks < MIN_CHARGE || !(entity instanceof Player player)) {
            return;
        }
        if (SpellCastHelper.cast(spellId(), spellLevel(), player, player)) {
            ToolDamageUtil.damage(tool, 1, player, null);
        }
    }
}
