package com.galtc2.modifiers;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * 天津罪 (tianjin_zui) —— 材料特性。
 * <p>
 * <b>手持或穿在身上</b>时展开一片「禁飞领域」：半径 {@value #RADIUS} 格内的生物一律飞不起来，
 * 正在飞的会被打下来。领域半径随等级每级 {@value #RADIUS_PER_LEVEL} 格。
 * <ul>
 *   <li>玩家：直接掐掉 {@code abilities.flying}（<b>不动</b> {@code mayfly}）——
 *       所以离开范围立刻就能重新起飞，不用去恢复任何状态；</li>
 *   <li>其他生物：关掉 {@code noGravity}，并对悬空的目标持续施加向下的位移。</li>
 * </ul>
 * <b>为什么生物要「施加位移」而不是「压速度」</b>：会飞的怪分两类，
 * <ul>
 *   <li><b>靠 {@code noGravity} 悬停的</b>（例如 BOMD 的暗夜巫妖）—— 把 {@code noGravity} 关掉，
 *       重力自己会把它拽下来，这一条最有效；</li>
 *   <li><b>靠 AI 驱动速度飞的</b>（例如凋零、幻翼）—— 它们每 tick 都在 {@code aiStep} 里把
 *       {@code deltaMovement} 重新设成"朝目标/向上"，我们写在 tick 末尾的 y 速度<b>下一帧就被覆盖</b>，
 *       等于白做（这正是"暗夜巫妖掉下来了、凋零却纹丝不动"的原因）。
 *       所以这里改成直接 {@code move(...)} 推一段向下的位移，绕开速度那条被 AI 把守的路。</li>
 * </ul>
 * <b>注意</b>：把位置逻辑写死在别处的 Boss 仍可能压不住，那种得针对具体实体单独处理，
 * 这里只做通用的那一层。
 */
public class TianjinZuiModifier extends Modifier implements InventoryTickModifierHook {

    /** 基础禁飞半径（格） */
    private static final double RADIUS = 10.0;
    /** 每级追加的半径（格） */
    private static final double RADIUS_PER_LEVEL = 2.0;
    /** 每次把悬空目标往下推多少格 —— 要盖过它的爬升，否则只是"上升变慢" */
    private static final double PULL_DOWN = 0.4;
    /** 每隔几 tick 撒一次粒子（每 tick 撒太吵） */
    private static final int PARTICLE_INTERVAL = 4;

    /** 实体持久数据里记录「本刻已经扫描过」，避免穿多件该材料时重复遍历 */
    private static final String KEY_LAST_TICK = "galtc2_tianjin_zui_tick";

    public TianjinZuiModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world,
                                LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot,
                                ItemStack stack) {
        // 只认「拿在手上」或「穿在装备槽上」；躺在背包/快捷栏里不生效
        if (world.isClientSide || (!isSelected && !isCorrectSlot) || !holder.isAlive()) {
            return;
        }
        // 一次遍历就够，穿四件护甲会把这个钩子调四次
        CompoundTag data = holder.getPersistentData();
        int now = holder.tickCount;
        if (data.getInt(KEY_LAST_TICK) == now) {
            return;
        }
        data.putInt(KEY_LAST_TICK, now);

        double radius = radiusFor(modifier.getEffectiveLevel());
        AABB area = holder.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != holder && e.isAlive());
        // 没东西要处理时早点退，别白算
        if (targets.isEmpty()) {
            return;
        }

        ServerLevel serverLevel = world instanceof ServerLevel level ? level : null;
        boolean showParticles = serverLevel != null && holder.tickCount % PARTICLE_INTERVAL == 0;
        for (LivingEntity target : targets) {
            if (ground(target) && showParticles) {
                // 被按下来的时候给点粒子，让玩家看得见领域在生效
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        2, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    /**
     * 给定等级对应的领域半径（格）。
     * <p>
     * 公开出来是因为「天津罪」的另一个特性（{@code tianjin_zui_no_spawn}，禁止范围内刷怪）
     * 要在别的类里用同一套尺度，见 {@code com.galtc2.event.TianjinZuiSpawnBanEvents}。
     */
    public static double radiusFor(float level) {
        return RADIUS + (level - 1.0F) * RADIUS_PER_LEVEL;
    }

    /**
     * 把一只生物按回地面，返回是否真的动了手（用来决定要不要撒粒子）。
     * <p>
     * 对玩家只改 {@code flying} 不改 {@code mayfly} —— 这样不持有状态、也不用担心
     * "离开范围后没能恢复飞行权限"。
     */
    private static boolean ground(LivingEntity entity) {
        if (entity instanceof Player player) {
            // 创造/旁观模式的飞行是调试用的，不干预
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
            boolean did = false;
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                did = true;
            }
            if (player.isFallFlying()) {
                player.stopFallFlying();
                did = true;
            }
            return did;
        }

        boolean did = false;
        // 靠 noGravity 悬停的（暗夜巫妖这类）：关掉就自己往下掉
        if (entity.isNoGravity()) {
            entity.setNoGravity(false);
            did = true;
        }
        // 靠 AI 驱动速度飞的（凋零、幻翼这类）：压速度会被下一帧的 aiStep 覆盖，
        // 所以直接推一段向下的位移。move() 带碰撞，不会把人推进方块里。
        if (!entity.onGround()) {
            entity.move(MoverType.SELF, new Vec3(0.0D, -PULL_DOWN, 0.0D));
            did = true;
        }
        return did;
    }
}
