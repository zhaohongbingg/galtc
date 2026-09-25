package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.init.JerotesDamageTypes;
import com.jerotes.jerotes.init.JerotesMobEffects;
import com.jerotes.jerotes.util.AttackFind;
import com.jerotes.jerotes.util.Main;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * 火力倾泻者·链锯(chainsaw)
 * 复刻 J村 链锯基类：长按右键持续对前方范围造成 20% 攻击伤害并施加流血。
 * 每次命中消耗 1 点耐久。
 * 占用能力槽，且不兼容其他武器能力强化。
 */
public class ChainsawModifier extends Modifier implements GeneralInteractionModifierHook, ValidateModifierHook {

    /** 每口伤害 = 武器攻击力 × 0.2 */
    private static final float DAMAGE_MULTI = 0.2f;
    private static final float REACH = 3.0f;
    private static final int BLEED_TICKS = 60;
    private static final int DURABILITY_COST = 1;

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
        return 72000;
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAnim.BLOCK;
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel) || !entity.isAlive()) {
            return;
        }
        InteractionHand hand = entity.getUsedItemHand();
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

        float reach = REACH;
        AABB area = entity.getBoundingBox().inflate(reach);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity target : targets) {
            if (target == entity || !target.isAlive()) {
                continue;
            }
            if (target.distanceTo(entity) > reach * 2.0f) {
                continue;
            }
            if (AttackFind.FindCanNotAttack(entity, target) || !Main.canSee(target, entity)) {
                continue;
            }
            DamageSource source = AttackFind.findDamageType(entity, JerotesDamageTypes.BYPASSES_COOLDOWN_MELEE, entity);
            float damage = ToolAttackUtil.getAttributeAttackDamage(tool, entity, slot) * DAMAGE_MULTI;
            if (target.hurt(source, damage)) {
                target.addEffect(new MobEffectInstance(JerotesMobEffects.BLEEDING.get(), BLEED_TICKS, 0, false, false), entity);
                ToolAttackUtil.spawnAttackParticle(ParticleTypes.CRIT, target, 0.5);
                ToolDamageUtil.damage(tool, DURABILITY_COST, entity, null);
            }
        }
    }
}
