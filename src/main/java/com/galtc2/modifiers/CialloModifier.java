package com.galtc2.modifiers;

import com.galtc2.Register.galtc2Sounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

public class CialloModifier extends Modifier implements MeleeHitModifierHook, ProjectileHitModifierHook {

    /** 每支箭只播一次：穿射箭命中多个目标时不至于叠成一串音效 */
    private static final ResourceLocation KEY_ARROW_DONE = ResourceLocation.fromNamespaceAndPath("galtc2", "ciallo_arrow_done");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (damageDealt <= 0) return;

        LivingEntity attacker = context.getAttacker();
        if (attacker == null || attacker.level().isClientSide) return;
        if (!(attacker instanceof Player player)) return;

        play(player, player.getX(), player.getY(), player.getZ());
    }

    /**
     * 箭矢命中：播同一套音效，但<b>播在目标位置</b>（近战是播在玩家位置）。
     * <p>
     * 每支箭只播一次（{@value #KEY_ARROW_DONE}，存在箭矢的 persistent data 上）：
     * 音效本身不是数值，但穿射穿 4 只怪连着响 4 遍会很难听，也没必要。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null) return false;
        if (attacker.level().isClientSide()) return false;
        if (!(attacker instanceof Player)) return false;
        if (persistentData.contains(KEY_ARROW_DONE)) return false;
        persistentData.putBoolean(KEY_ARROW_DONE, true);

        play(attacker, target.getX(), target.getY(), target.getZ());
        return false;
    }

    /** 播放自定义 Ciallo 音效（如果 OGG 文件存在）+ 一个原版音效做验证（Hook 触发确认） */
    private static void play(LivingEntity source, double x, double y, double z) {
        source.level().playSound(null, x, y, z,
                galtc2Sounds.CIALLO.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        source.level().playSound(null, x, y, z,
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
    }
}
