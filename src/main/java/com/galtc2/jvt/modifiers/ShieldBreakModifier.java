package com.galtc2.jvt.modifiers;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;

import javax.annotation.Nullable;

/**
 * 不祥标枪·破盾(shield_break)
 * 复刻 J村 不祥标枪：弹射物命中后暂时停用目标的盾牌。
 * 占用能力槽，且不兼容其他武器能力强化。
 */
public class ShieldBreakModifier extends Modifier implements ProjectileHitModifierHook, ValidateModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_HIT, ModifierHooks.VALIDATE);
    }

    @Override
    public Component validate(IToolStackView tool, ModifierEntry modifier) {
        return WeaponEnhancements.validateExclusive(tool, modifier);
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player player) {
            player.disableShield(true);
        } else {
            target.stopUsingItem();
        }
        return false;
    }
}
