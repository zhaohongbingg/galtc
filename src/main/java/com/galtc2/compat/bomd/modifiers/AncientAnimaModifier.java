package com.galtc2.compat.bomd.modifiers;

import com.cerbon.bosses_of_mass_destruction.entity.util.ProjectileThrower;
import com.cerbon.bosses_of_mass_destruction.projectile.MagicMissileProjectile;
import com.cerbon.bosses_of_mass_destruction.projectile.comet.CometProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * 星空下的记忆 (ancient_anima) —— 祸乱鬼魅 联动强化。
 * <p>
 * 右键随机射出暗夜巫妖的两种弹幕之一：
 * <ul>
 *   <li><b>坠星</b> —— BOMD 的 {@code comet}（Lich 的 {@code CometAction} 用的就是这个）；</li>
 *   <li><b>霜星</b> —— BOMD 的 {@code magic_missile}（Lich 的 {@code VolleyAction} 用的那个）。</li>
 * </ul>
 * 弹道交给 {@link ProjectileThrower} 算（speed 1.6 / gravityCompensation 0.2，与 BOMD 同口径），
 * 但<b>枪口贴准星</b>：从眼睛沿视线前移半格出膛，不用 BOMD 给 3 格高 Lich 准备的
 * {@code getCometLaunchOffset() = (0,2,0)}（套在玩家身上会从头顶冒出来）。
 * 命中点用 {@code impactAction} 里的 {@code explode} 兑现伤害。
 * <p>
 * <b>注意</b>：{@link CometProjectile} 重写了 {@code entityHit} 且<b>没有</b>调用 super，
 * 所以它自己一点伤害都不打 —— 爆炸/伤害全靠构造时传进去的回调，回调为空的话
 * 坠星命中后会「什么都不发生直接消失」。
 */
public class AncientAnimaModifier extends Modifier implements GeneralInteractionModifierHook {

    /** 出膛初速（BOMD 的 Lich 坠星用 1.6） */
    private static final float VELOCITY = 1.6f;
    /** 散布；BOMD 的坠星是 0，这里留一点点，允许"指哪打哪但有偏差" */
    private static final float DIVERGENCE = 1.0f;
    /** 重力补偿系数（BOMD 用 0.2：按水平距离把弹道抬高，抵消下坠，保证落在瞄准点上） */
    private static final double GRAVITY_COMPENSATION = 0.2;
    /**
     * 枪口相对眼睛沿视线前移多少格。
     * <p>
     * 这里<b>不再</b>用 BOMD 的 {@code getCometLaunchOffset() = (0,2,0)} —— 那个是给 3 格高的
     * Lich 用的，套在玩家身上会让坠星从头顶冒出来、明显高于准星。改成贴着准星出膛。
     */
    private static final double MUZZLE_OFFSET = 0.5;
    /** 瞄准点取视线前方多少格 */
    private static final double AIM_DISTANCE = 30.0;
    /** 坠星落地爆炸威力（BOMD 取自 LichConfig，这里给个固定值） */
    private static final float COMET_EXPLOSION = 2.0f;

    public AncientAnimaModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player,
                                       InteractionHand hand, InteractionSource source) {
        if (source != InteractionSource.RIGHT_CLICK || tool.isBroken()) {
            return InteractionResult.PASS;
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            Projectile projectile = player.getRandom().nextBoolean()
                    ? createComet(player, serverLevel)
                    : createMissile(player, serverLevel);
            launch(projectile, player);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 坠星：命中点炸一下。
     * <p>
     * 这正是 BOMD 的 {@code CometAction} 里的写法 —— {@link CometProjectile} 只负责飞到命中点，
     * 伤害完全由这个回调兑现。用 {@code ExplosionInteraction.MOB} 不破坏方块。
     */
    private static CometProjectile createComet(Player player, ServerLevel level) {
        return new CometProjectile(player, level,
                pos -> level.explode(player, pos.x, pos.y, pos.z, COMET_EXPLOSION, Level.ExplosionInteraction.MOB),
                List.<EntityType<?>>of());
    }

    /** 霜星：{@link MagicMissileProjectile} 自带命中伤害，回调留空沿用投射物本身的表现。 */
    private static MagicMissileProjectile createMissile(Player player, ServerLevel level) {
        return new MagicMissileProjectile(player, level, target -> {
        }, List.<EntityType<?>>of());
    }

    /**
     * 从准星位置出膛：枪口 = 眼睛沿视线前移 {@value #MUZZLE_OFFSET} 格，瞄准点 = 眼睛沿视线前方
     * {@value #AIM_DISTANCE} 格，方向与重力补偿都交给 {@link ProjectileThrower}
     * （它会把弹道按 gravityCompensation 抬高，让落点落在瞄准点上）。
     */
    private static void launch(Projectile projectile, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 muzzle = player.getEyePosition().add(look.scale(MUZZLE_OFFSET));
        projectile.setPos(muzzle.x, muzzle.y, muzzle.z);
        Vec3 aim = player.getEyePosition().add(look.scale(AIM_DISTANCE));
        new ProjectileThrower(() -> new ProjectileThrower.ProjectileData(
                projectile, VELOCITY, DIVERGENCE, GRAVITY_COMPENSATION)).throwProjectile(aim);
    }
}
