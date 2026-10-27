package com.galtc2.compat.aetherbedrock;

import com.aetherbedrockmobsreborn.entity.GolemSkillEntity;
import com.aetherbedrockmobsreborn.entity.NagaSkillEntity;
import com.aetherbedrockmobsreborn.entity.SpiritSkillEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 直接调用「天境基石生物重制」自己的技能实体出特效，而不是用原版粒子凑。
 * <p>
 * 三个技能实体（源码 {@code entity/SpiritSkillEntity}、{@code entity/NagaSkillEntity}、
 * {@code entity/GolemSkillEntity}）都是原模组 Boss 施法用的，客户端还有配套渲染器
 * （大剑模型 / 落石 / 晶笼柱）与粒子（{@code ModParticles} 的自定义冲击环、碎晶）。
 * 这里只做一件事：以玩家为 owner 把它们生出来，剩下的表现与伤害全部交给原模组。
 * <p>
 * <b>伤害口径</b>：这些实体命中时走 {@code BossSkillDamage.calculate(owner, skill)}，
 * 即「玩家面板攻击力 × 模组配置倍率」（{@code config/aether_bedrock_mobs_reborn-common.toml}
 * 的 {@code boss_skill_multipliers}，例如天剑默认 12/11、巨石 10/6、晶笼破土 6/8）。
 * 所以本模组的联动强化不再自己结算伤害，避免和原技能叠加成双倍。
 * <p>
 * <b>注意</b>：原技能实体的 {@code canHit} 会排除 {@code RebornMobEntity}（Boss 不打 Boss），
 * 因此这几个联动强化对天境本家的 Boss 无效 —— 这是原模组的既定行为。
 */
public final class AetherBedrockEffects {

    /** 天剑：和灯壶之灵本体一致的预警节奏（首柄 16 tick 后落剑，多柄由调用方错开） */
    private static final int SPIRIT_SWORD_DELAY = 16;

    /** 巨石：先从目标上方这么高的位置生成，再按飞行 tick 反推初速度 */
    private static final double NAGA_BOULDER_HEIGHT = 4.0D;
    private static final int NAGA_BOULDER_FLIGHT = 6;

    /** 晶笼：破土预警 2 tick，和魔像的晶笼节奏接近 */
    private static final int GOLEM_CAGE_DELAY = 2;

    /** 晶刺冲锋：玩家身后留下的一串地刺（距离 / 初始延迟 / 大小），模仿魔像冲撞时沿路留下的晶刺 */
    private static final double[] SPIKE_TRAIL_DISTANCE = {1.0D, 2.0D, 3.0D, 4.0D, 5.0D, 6.0D, 7.0D};
    private static final int[] SPIKE_TRAIL_DELAY = {0, 1, 2, 4, 6, 8, 10};
    private static final float[] SPIKE_TRAIL_SCALE = {0.90F, 0.85F, 0.80F, 0.75F, 0.70F, 0.65F, 0.60F};
    /** 每根地刺的横向随机偏移，避免排成一条死板的直线 */
    private static final double SPIKE_TRAIL_JITTER = 0.35D;
    /** 地刺找地面的最大下探深度；找不到（悬崖/深水）就跳过这一根，不生成悬空刺 */
    private static final int SPIKE_GROUND_SEARCH = 5;

    private AetherBedrockEffects() {
    }

    /**
     * 召来灯壶之灵的天剑：落点上方 14 格悬停 → 匀速下落 → 插地爆炸。
     *
     * @param ground 落点（一般取目标脚下/身体位置，y 为剑尖最终落到的地面高度）
     * @param delay  预警 tick（实体先出现收缩预警环，到点开始下落）
     */
    public static boolean spiritSwordfall(ServerLevel level, LivingEntity owner, Vec3 ground, int delay) {
        Vec3 spawnPos = new Vec3(ground.x, ground.y + SpiritSkillEntity.SWORD_HEIGHT, ground.z);
        return SpiritSkillEntity.spawn(level, owner, SpiritSkillEntity.SWORD,
                spawnPos, Vec3.ZERO, ground.y, delay, null) != null;
    }

    /** 默认预警节奏（单柄 16 tick） */
    public static boolean spiritSwordfall(ServerLevel level, LivingEntity owner, Vec3 ground) {
        return spiritSwordfall(level, owner, ground, SPIRIT_SWORD_DELAY);
    }

    /**
     * 召来天境娜迦的巨石：从目标上方落下，落地触发秘符冲击环 + 震起地面 + 碎石 + 爆炸。
     *
     * @param center 目标位置（内部会向下找地面并把落点贴到地面）
     */
    public static void nagaQuake(ServerLevel level, LivingEntity owner, Vec3 center) {
        Vec3 landing = groundBelow(level, center.x, center.y + 1.0D, center.z);
        Vec3 from = landing.add(0.0D, NAGA_BOULDER_HEIGHT, 0.0D);
        // 头顶有方块时压低生成高度，避免巨石一出生就撞墙
        if (!level.getBlockState(BlockPos.containing(from)).isAir()) {
            from = landing.add(0.0D, 2.0D, 0.0D);
        }
        NagaSkillEntity.spawnBoulder(level, owner, from, landing, NAGA_BOULDER_FLIGHT);
    }

    /**
     * 在攻击者脚下升起水晶石傀儡的晶笼：破土伤害 + 停留期间持续伤害 + 碎晶特效。
     */
    public static void golemCage(ServerLevel level, LivingEntity owner, LivingEntity attacker) {
        Vec3 ground = groundBelow(level, attacker.getX(), attacker.getY() + 1.0D, attacker.getZ());
        GolemSkillEntity.spawn(level, owner, GolemSkillEntity.CAGE, ground, GOLEM_CAGE_DELAY, 1.0F, false);
    }

    /**
     * 晶岩魔像的「冲撞」留下的地刺：沿移动方向的反方向，在玩家身后踏出一串
     * {@link GolemSkillEntity#SPIKE}（破土伤害 + 碎晶特效 + 晶刺音效，全部由原模组演出）。
     * <p>
     * 距离从 1 格排到 7 格，共 7 根，越远越小、依次破土；
     * 每根带一点横向随机偏移，看起来像一路冲过去踩出来的，而不是一条直线。
     * 找不到地面（悬崖/深水）的那一根直接跳过，不会出现悬空晶刺。
     *
     * @param movement 触发那一刻的速度，只用其水平分量决定方向
     */
    public static void golemSpikeCharge(ServerLevel level, LivingEntity owner, Vec3 movement) {
        Vec3 horizontal = new Vec3(movement.x, 0.0D, movement.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 back = horizontal.normalize().scale(-1.0D);
        // 横向单位向量：用来做横向抖动
        Vec3 side = new Vec3(-back.z, 0.0D, back.x);
        for (int i = 0; i < SPIKE_TRAIL_DISTANCE.length; i++) {
            double jitter = (level.random.nextDouble() - 0.5D) * 2.0D * SPIKE_TRAIL_JITTER;
            Vec3 behind = owner.position()
                    .add(back.scale(SPIKE_TRAIL_DISTANCE[i]))
                    .add(side.scale(jitter));
            Vec3 ground = groundBelowOrNull(level, behind.x, owner.getY() + 1.0D, behind.z, SPIKE_GROUND_SEARCH);
            if (ground == null) {
                continue;
            }
            // 只有第一根出声，后面的静音，避免一次触发一串音效
            GolemSkillEntity.spawn(level, owner, GolemSkillEntity.SPIKE, ground,
                    SPIKE_TRAIL_DELAY[i], SPIKE_TRAIL_SCALE[i], i != 0);
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

    /** 同上，但只向下找 {@code maxDepth} 格；找不到地面返回 {@code null}（用于跳过悬崖/深水） */
    @Nullable
    private static Vec3 groundBelowOrNull(ServerLevel level, double x, double y, double z, int maxDepth) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, Mth.floor(y), z);
        for (int i = 0; i <= maxDepth; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir()) {
                return Vec3.atBottomCenterOf(cursor.above());
            }
            cursor.move(0, -1, 0);
            if (cursor.getY() < level.getMinBuildHeight()) {
                break;
            }
        }
        return null;
    }
}
