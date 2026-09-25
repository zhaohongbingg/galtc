package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.util.AttackFind;
import com.jerotes.jerotes.util.EntityAndItemFind;
import com.jerotes.jerotes.util.Main;
import com.jerotes.jerotesvillage.init.JerotesVillageDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
 * 疯狂之斧(crazy_axe)
 * 复刻 J村 疯狂之斧：长按右键进入旋风(最长 240 tick)。
 * - 每 tick 向前冲刺；
 * - 每 5 tick 对周围敌人造成一次 AX_CRAZY_ATTACK 伤害，并吸取造成伤害的 1/3；
 * - 击退并把小/中型生物瞬移到侧后方；
 * - 破坏周围可破坏(斧类/低硬度)方块；
 * - 自身获得短暂抗性提升。
 * 释放后：冷却 360 tick、扣 8 点耐久。
 * 已按要求去掉"给命中目标加buff"的部分。
 * 占用能力槽，且不兼容其他武器能力强化。
 */
public class CrazyAxeModifier extends Modifier implements GeneralInteractionModifierHook, ValidateModifierHook {

    private static final int USE_DURATION = 240;
    private static final int HIT_INTERVAL = 5;
    private static final int RELEASE_COOLDOWN = 360;
    private static final int TOOL_DAMAGE_ON_RELEASE = 8;
    private static final float REACH = 3.0f;
    private static final float LIFESTEAL_DIVISOR = 3.0f;
    private static final double DASH_SPEED = 0.14;
    private static final double TELEPORT_DISTANCE = 4.5;

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
        return UseAnim.CUSTOM;
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel) || !entity.isAlive()) {
            return;
        }

        // 每 tick 向前冲刺
        if (entity.onGround()) {
            float yaw = entity.getYRot();
            float pitch = entity.getXRot();
            double dx = -Mth.sin(yaw * 0.017453292F) * Mth.cos(pitch * 0.017453292F);
            double dy = -Mth.sin(pitch * 0.017453292F);
            double dz = Mth.cos(yaw * 0.017453292F) * Mth.cos(pitch * 0.017453292F);
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 0) {
                entity.push(dx * DASH_SPEED / len * 2.0, dy * DASH_SPEED / len * 2.0, dz * DASH_SPEED / len * 2.0);
            }
        }

        if (timeLeft % HIT_INTERVAL != 0) {
            return;
        }

        // 自身短时抗性提升
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 2), entity);
        entity.swing(InteractionHand.MAIN_HAND, true);

        // 破坏周围方块(斧类/低硬度)
        AABB breakArea = entity.getBoundingBox().inflate(1.0, 0.5, 1.0).move(0.0, 0.5, 0.0);
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(breakArea.minX), Mth.floor(breakArea.minY), Mth.floor(breakArea.minZ),
                Mth.floor(breakArea.maxX), Mth.floor(breakArea.maxY), Mth.floor(breakArea.maxZ))) {
            BlockState state = serverLevel.getBlockState(pos);
            float hardness = state.getDestroySpeed(serverLevel, pos);
            if (hardness < 0.0f || hardness > 5.0f) {
                continue;
            }
            if (!state.is(BlockTags.MINEABLE_WITH_AXE) && hardness > 2.0f) {
                continue;
            }
            if (!Main.canSee(pos.getCenter(), entity)) {
                continue;
            }
            serverLevel.destroyBlock(pos, true, entity);
        }

        // 范围绞杀
        InteractionHand hand = entity.getUsedItemHand();
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(REACH));
        for (LivingEntity target : targets) {
            if (target == entity || !target.isAlive()) {
                continue;
            }
            if (target.distanceTo(entity) > REACH * 4.0f) {
                continue;
            }
            if (AttackFind.SameFactionAvoidDamage(entity, target, false)) {
                continue;
            }
            if (!target.hasLineOfSight(entity) || !Main.canSee(target, entity)) {
                continue;
            }
            DamageSource source = AttackFind.findDamageType(entity, JerotesVillageDamageTypes.AX_CRAZY_ATTACK, entity);
            float damage = ToolAttackUtil.getAttributeAttackDamage(tool, entity, slot);
            float before = target.getHealth();
            if (target.hurt(source, damage)) {
                float dealt = before - target.getHealth();
                if (dealt > 0) {
                    entity.heal(dealt / LIFESTEAL_DIVISOR);
                }
                // 击退 + 瞬移小/中型生物
                if (!EntityAndItemFind.isNoSpecialKnockback(target.getType()) && (Main.mobSizeSmall(target) || Main.mobSizeMedium(target))) {
                    if (entity.onGround()) {
                        target.setOnGround(false);
                    }
                    double angleYaw = Math.PI / 2.0;
                    double radians = Math.toRadians(entity.getYRot()) + angleYaw;
                    double x = entity.getX() + TELEPORT_DISTANCE * Math.cos(radians);
                    double z = entity.getZ() + TELEPORT_DISTANCE * Math.sin(radians);
                    if (!target.isShiftKeyDown()) {
                        target.teleportTo(x, entity.getY(), z);
                    }
                }
            }
        }
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        finishUse(tool, entity);
    }

    @Override
    public void onFinishUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity) {
        finishUse(tool, entity);
    }

    /** 释放/结束时：冷却 + 扣耐久 */
    private void finishUse(IToolStackView tool, LivingEntity entity) {
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            player.getCooldowns().addCooldown(tool.getItem(), RELEASE_COOLDOWN);
        }
        ToolDamageUtil.damage(tool, TOOL_DAMAGE_ON_RELEASE, entity, null);
    }
}
