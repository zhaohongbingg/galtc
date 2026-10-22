package com.galtc2.compat.bomd.modifiers;

import com.cerbon.bosses_of_mass_destruction.entity.custom.gauntlet.LaserAction;
import com.cerbon.bosses_of_mass_destruction.sound.BMDSounds;
import com.cerbon.bosses_of_mass_destruction.util.BMDUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 祐天寺美汐 (blazing_eye) —— 祸乱鬼魅 联动强化。
 * <p>
 * 按住右键射出一道激光，照下界铁掌那套来做：射线打到哪就结算到哪，
 * 而不是"发射一个会飞的弹丸"。
 * <ul>
 *   <li>射线从玩家眼睛沿视线出发，长度走 BOMD 的口径
 *       （{@link LaserAction#extendLaser}：眼睛 + 单位方向 × 30 格），被方块挡住就<b>截断</b>；</li>
 *   <li>每 {@value #DAMAGE_INTERVAL} tick 结算一次，所以按住不放是持续掉血；</li>
 *   <li>命中判定仍用 BOMD 的 {@link BMDUtils#findEntitiesInLine} 找线段上的实体
 *       （穿透式，线上所有活体都会吃）；伤害源换成了 {@code galtc2:linkage}
 *       （见 {@link BomdHelper#linkageHit}）—— BOMD 的 {@code shield_piercing} 没带
 *       {@code bypasses_cooldown}，会被 20 tick 无敌帧吃掉，导致整段持续射击只生效第一下；</li>
 *   <li>起手放 BOMD 的蓄力音效 {@link BMDSounds#GAUNTLET_LASER_CHARGE}。</li>
 * </ul>
 * <b>光束本体在客户端画</b>：见 {@code com.galtc2.compat.bomd.client.BlazingEyeLaserRenderer}
 * —— 那边和 BOMD 的 {@code GauntletLaserRenderer} 一样，用
 * {@code gauntlet_beam.png} + {@code RenderType.entityCutoutNoCull} + {@code VanillaCopies.renderBeam}
 * 画一条实心光束。所以这个类只管服务端的判定与伤害，不再往世界里撒粒子。
 * <p>
 * <b>关于资源</b>：全程"运行时引用"，没有把 BOMD 的任何文件复制进 galtc2，
 * 所以不触发 LGPL-3.0 的分发义务；没装 BOMD 时这些类也根本不会被加载。
 * <p>
 * 下界铁掌的 {@link LaserAction} 本身是实例类、构造要 {@code GauntletEntity}，
 * 且 {@code applyLaser} / {@code applyLaserToEntities} 都是 private，外部调不动；
 * 但它依赖的零件（{@code extendLaser} / {@code BMDUtils.findEntitiesInLine} /
 * {@code BMDUtils.shieldPiercing}）全是 public static，所以这里直接拿它们拼一条同款射线。
 */
public class BlazingEyeModifier extends Modifier implements GeneralInteractionModifierHook {

    /** 一次右键最多持续多久（tick） */
    private static final int USE_DURATION = 40;
    /** 每隔几 tick 结算一次伤害 */
    private static final int DAMAGE_INTERVAL = 2;
    /** 瞄点取视线前方多少格；真正的激光长度由 {@link LaserAction#extendLaser} 固定为 30 格 */
    private static final double AIM_DISTANCE = 30.0;
    /**
     * 每次结算的伤害 = 施法者攻击力面板 × 这个系数。
     * <p>
     * BOMD 的激光用的是 {@code gauntletLaserDamage} 这种固定值，这里改成跟着武器面板走
     * （一半，和地刺同一个口径），换成更好的工具时激光也会更疼。
     */
    private static final float DAMAGE_SCALE = 0.5f;

    public BlazingEyeModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player,
                                       InteractionHand hand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK && !tool.isBroken()) {
            GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
            // 蓄力音效：BOMD 的下界铁掌开激光时放的就是这个
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    BMDSounds.GAUNTLET_LASER_CHARGE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
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
        if (!(entity instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (timeLeft % DAMAGE_INTERVAL != 0) {
            return;
        }
        fire(level, player, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_SCALE);
    }

    /**
     * 打一条激光出去并结算伤害。
     * <p>
     * 光束的<b>视觉</b>不在这里 —— 客户端 {@code BlazingEyeLaserRenderer} 会按同样的
     * 起点/终点/截断规则画那条实心光束。这边只负责把伤害结算掉。
     */
    private static void fire(ServerLevel level, Player player, float damage) {
        Vec3 start = player.getEyePosition();
        // 长度口径交给 BOMD：extendLaser 内部就是「眼睛 + 单位方向 × 30 格」
        Vec3 aim = start.add(player.getLookAngle().scale(AIM_DISTANCE));
        Vec3 farEnd = LaserAction.extendLaser(player, aim);

        // 被方块挡住就截断（BOMD 的激光也是这么做的）
        BlockHitResult blockHit = level.clip(new ClipContext(start, farEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 beamEnd = blockHit.getType() == HitResult.Type.MISS ? farEnd : blockHit.getLocation();

        // 沿线找实体：用 BOMD 激光同款的 BMDUtils.findEntitiesInLine（穿透式，线上所有活体都吃）
        for (Entity candidate : BMDUtils.findEntitiesInLine(level, start, beamEnd, player)) {
            if (candidate instanceof LivingEntity living && living.isAlive()) {
                // 伤害源换成联动伤害类型（带 bypasses_cooldown）。BOMD 自己的 shield_piercing 会被
                // 20 tick 无敌帧吃掉，而这里是每 2 tick 结算一次 —— 用它的话整段持续射击只会生效
                // 第一下，表现就是"伤害恒定 1.5"。
                BomdHelper.linkageHit(level, player, living, damage);
            }
        }
    }
}
