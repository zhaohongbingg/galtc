package com.galtc2.modifiers;

import com.galtc2.Register.galtc2Sounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class CialloModifier extends Modifier implements MeleeHitModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (damageDealt <= 0) return;

        LivingEntity attacker = context.getAttacker();
        if (attacker == null || attacker.level().isClientSide) return;
        if (!(attacker instanceof Player player)) return;

        // 播放自定义 Ciallo 音效（如果 OGG 文件存在）
        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                galtc2Sounds.CIALLO.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);

        // 额外播放一个原版音效做验证（Hook 触发确认）
        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS,
                1.0F, 2.0F);
    }
}
