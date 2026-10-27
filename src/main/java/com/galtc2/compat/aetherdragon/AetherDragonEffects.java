package com.galtc2.compat.aetherdragon;

import com.aetherdragon.entity.DragonSkillEntity;
import com.aetherdragon.registry.ModParticles;
import com.aetherdragon.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 天境巨龙特效库的调用点（天镜龙魂 材料的龙系特性用）。
 * <p>
 * <b>攻击特效</b>：用服务端粒子包直接生成原模组的碎晶/龙息粒子 —— 这些粒子的 Provider
 * （{@code ShardParticle} / {@code FlameParticle}）把生成参数当<b>初速度</b>用，所以
 * {@code ServerLevel.sendParticles(...)} 这条纯函数调用就能正确表现：
 * <ul>
 *   <li>{@code DRAGON_SHARD} —— 蓝 / 白 / 金三张精灵图随机取，爆一堆旋转下落的龙鳞碎晶；</li>
 *   <li>{@code FIRE} —— 原模组的青绿色龙息火焰帧；</li>
 *   <li>{@code PYRE_EMBER} —— 橙金余烬（截图里那些小橙色光点）。</li>
 * </ul>
 * 音效走原模组音效表的 {@code dragon_breath_fire} / {@code dragon_claw_hit}。
 * <p>
 * 注意：扩展型冲击环（{@code ShockwaveParticle}）与剑光贴片（{@code DecalParticle}）的生成参数
 * 被客户端当成「半径/寿命/旋转」用，服务端粒子包传不进去；要那两个必须走实体事件通道
 * （且要等实体被客户端跟踪后再广播，否则事件包没有接收者，只剩声音）。
 * <p>
 * <b>龙刺</b>：{@link DragonSkillEntity#SPIKE}（大号滴水石锥），远程特性用，走原模组公开的
 * {@link DragonSkillEntity#spawn} 函数。
 */
public final class AetherDragonEffects {

    /** 特效整体上下偏移的格数（0 = 目标身体中心；负数偏低、正数偏高） */
    private static final double FX_Y_OFFSET = 0.0D;

    /** 碎晶数量 / 散布 / 初速度 */
    private static final int SHARD_COUNT = 30;
    private static final double SHARD_SPREAD = 0.8D;
    private static final double SHARD_SPEED = 0.35D;

    /** 青绿龙息火焰 */
    private static final int FIRE_COUNT = 14;
    private static final double FIRE_SPREAD = 0.5D;
    private static final double FIRE_SPEED = 0.06D;

    /** 橙金余烬 */
    private static final int EMBER_COUNT = 10;
    private static final double EMBER_SPREAD = 0.6D;
    private static final double EMBER_SPEED = 0.12D;

    /** 龙刺落点：命中点周围的 5×5 范围内的 5 个位置（中心 + 四角） */
    private static final double[][] SPIKE_OFFSETS = {
            {0.0D, 0.0D},
            {-2.0D, -2.0D},
            {2.0D, -2.0D},
            {-2.0D, 2.0D},
            {2.0D, 2.0D},
    };

    /** 第一根龙刺的预警 tick */
    private static final int SPIKE_BASE_DELAY = 2;
    /** 每多一根龙刺错开的 tick */
    private static final int SPIKE_DELAY_STEP = 2;

    private AetherDragonEffects() {
    }

    /**
     * 近战命中：在目标身上（下移 {@value #FX_Y_OFFSET} 格）爆开原模组的
     * 龙鳞碎晶（蓝/白/金）+ 青绿龙息 + 橙金余烬，并播放龙息 / 爪击音效。
     */
    public static void attackFx(ServerLevel level, LivingEntity attacker, LivingEntity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D + FX_Y_OFFSET, 0.0D);
        level.sendParticles(ModParticles.DRAGON_SHARD.get(),
                center.x, center.y, center.z, SHARD_COUNT, SHARD_SPREAD, SHARD_SPREAD, SHARD_SPREAD, SHARD_SPEED);
        level.sendParticles(ModParticles.FIRE.get(),
                center.x, center.y, center.z, FIRE_COUNT, FIRE_SPREAD, FIRE_SPREAD, FIRE_SPREAD, FIRE_SPEED);
        level.sendParticles(ModParticles.PYRE_EMBER.get(),
                center.x, center.y, center.z, EMBER_COUNT, EMBER_SPREAD, EMBER_SPREAD, EMBER_SPREAD, EMBER_SPEED);
        ModSounds.play(level, "dragon_breath_fire", center.x, center.y, center.z, 1.2F, 1.0F);
        ModSounds.play(level, "dragon_claw_hit", center.x, center.y, center.z, 0.9F, 1.1F);
    }

    /** 弹射物命中：在命中点周围 5×5 范围内依次降下 5 根龙刺 */
    public static void spawnSpikes(ServerLevel level, LivingEntity owner, Vec3 center) {
        for (int i = 0; i < SPIKE_OFFSETS.length; i++) {
            double x = center.x + SPIKE_OFFSETS[i][0];
            double z = center.z + SPIKE_OFFSETS[i][1];
            Vec3 ground = groundBelow(level, x, center.y + 1.0D, z);
            DragonSkillEntity.spawn(level, owner, DragonSkillEntity.SPIKE,
                    ground, Vec3.ZERO, ground.y, SPIKE_BASE_DELAY + i * SPIKE_DELAY_STEP, null);
        }
    }

    /** 从指定高度向下找第一个非空气方块的顶面中心；找不到就返回原高度 */
    private static Vec3 groundBelow(ServerLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, Mth.floor(y), z);
        for (int i = 0; i < 6; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir()) {
                return Vec3.atBottomCenterOf(cursor.above());
            }
            cursor.move(0, -1, 0);
            if (cursor.getY() < level.getMinBuildHeight()) {
                break;
            }
        }
        return new Vec3(x, Mth.floor(y), z);
    }
}
