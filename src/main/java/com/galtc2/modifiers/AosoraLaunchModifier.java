package com.galtc2.modifiers;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 苍彼（aosora）——近战命中后把目标向上抛起，最高点约 64 格，然后自然下坠。
 * <p>
 * 只对<b>手里的武器</b>生效：匠魂会把近战命中钩子同样派发给盔甲上的特性
 * （原版盔甲模块 Enderclearance 就是这么用的），所以这里不能信钩子传进来的
 * {@code modifier.getLevel()}，必须回到主/副手去确认拿着的是什么（见 {@link TraitLevels#heldLevel}）。
 * 只穿苍彼护甲、空手打怪不会触发。
 * <p>
 * 服务端权威：速度只在服务端设置，玩家目标的位移额外用 {@code hurtMarked} 立刻下发
 * （{@code ClientboundSetEntityMotionPacket}），客户端只做表现。
 * <p>
 * 抛起高度按 MC 1.20.1 的竖直运动模型反解，见 {@link #AOSORA_LAUNCH_VELOCITY} 的注释。
 */
public class AosoraLaunchModifier extends BaseModifiers {

    /** 苍彼特性 ID（材料 traits 里授予的就是这个）。 */
    public static final ModifierId AOSORA_TRAIT_ID = new ModifierId("galtc2", "aosora");

    /**
     * 抛起初速（格/tick）。
     * <p>
     * 推导：MC 1.20.1 的活体实体在空气中每 tick 先减重力 0.08，再乘空气阻尼 0.98，
     * 也就是 {@code v ← (v - 0.08) × 0.98}；纯竖直、从静止起抛时高度 = 各 tick 竖直位移之和，
     * 速度第一次 ≤ 0 时到达最高点。该递推有闭式解：
     * <pre>
     *   v_n = -3.92 + (v0 + 3.92) × 0.98^n        （-3.92 是终端速度）
     *   h   = Σ v_n（只累加到 v_n &gt; 0 为止，v0 = 4.02 时共 36 tick）
     * </pre>
     * <b>实测结论（2026-10-26 tester 探针，真实近战命中，僵尸 Y 12.00 → 80.18，之后自然坠回 12.00）：</b>
     * v0 = 4.18 时实测 ΔY = <b>68.18 格</b>，正好等于"先累加再衰减"这一口径的数值解 —— 说明引擎在命中当 tick
     * 就先按 v0 位移一次，之后才进入 {@code (v - 0.08) × 0.98} 的衰减。所以以该口径反解：
     * <pre>
     *   v0 = 4.00 → ≈ 63.5 格
     *   v0 = 4.02 → ≈ 64.1 格   ← 取这个，对准用户口径的 64 格
     *   v0 = 4.18 → ≈ 68.2 格（实测 68.18，计算吻合；比 64 高 6.4%，已下调）
     *   v0 = 4.50 → ≈ 76.7 格
     * </pre>
     * 作为对照："先衰减再累加位移"（不把 v0 计入第一 tick 位移）在 4.02 时只有 ≈ 60.0 格、在 4.18 时是 64.0 格，
     * 与实测不符，故不采用该口径。
     * 实测优先：本常量以 tester 的探针实测最高点为准；若改动后实测再次偏离 64 ± 8，
     * 按实测 ΔY 与上表的换算关系回填，不再凭公式猜。
     */
    public static final double AOSORA_LAUNCH_VELOCITY = 4.02D;

    /**
     * 远程（弓/弩）抛起初速：单独一个常量，<b>近战仍用 {@link #AOSORA_LAUNCH_VELOCITY}（4.02，≈64 格）</b>。
     * <p>
     * 为什么远程要小：近战要贴脸、有被打断的风险；箭矢是隔 30 格外点的，挑飞 64 格等于零风险清怪。
     * 目标值 32 格（用户 2026-10-27 口径：远程抛起砍半），按同一套<b>离散</b>竖直运动模型反解：
     * <pre>
     *   v_0 = v0；v_{n+1} = (v_n - 0.08) × 0.98（先减重力再乘空气阻尼）
     *   高度 h = Σ v_n（累加到 v_n ≤ 0 为止）
     * </pre>
     * 数值解（本仓库 Java 之外的快速复算，与实测口径一致）：
     * <pre>
     *   v0 = 1.500 → h = 12.13 格（旧值，实测 12 格左右）
     *   v0 = 2.635 → h = 32.00 格  ← 取这个，正好 32 格
     *   v0 = 4.020 → h = 64.05 格（近战，实测 68 格量级为含首 tick 位移的口径）
     * </pre>
     * 用户想要更夸张/更收敛都只改这一个常量，近战不受影响。
     */
    public static final double AOSORA_RANGED_VELOCITY = 2.635D;

    /** 竖直速度高于这个值就认为目标已经在上升途中，不再叠加（防止连击把怪打出世界高度）。 */
    private static final double RISING_THRESHOLD = 0.5D;

    /** 同一个目标两次抛起之间的最小间隔（tick），与上升判定一起兜住"同一 tick 多次命中"的情况。 */
    private static final long LAUNCH_COOLDOWN_TICKS = 20L;

    /** 去重表：目标 UUID → 上次抛起时的 gameTime。只在服务端主线程读写。 */
    private static final Map<UUID, Long> LAST_LAUNCH = new HashMap<>();
    /** 去重表超过这个大小就顺手清掉已经过期的条目，避免长期运行时无限增长。 */
    private static final int LAST_LAUNCH_CLEANUP_SIZE = 256;

    /**
     * 延后 1 tick 施加的抛起队列（key = 目标 UUID，同一目标同时只排一项）。
     * <p>
     * 为什么远程必须延后：{@code PROJECTILE_HIT} 在 {@code ProjectileImpactEvent} 里派发，
     * <b>早于箭矢伤害结算</b>；而箭矢伤害走 {@code AbstractArrow#onHitEntity → Entity#hurt}，
     * 原版 {@code LivingEntity#hurt} 会做受击击退并再写一次 {@code setDeltaMovement}（Y = 0.4），
     * 把这里刚写的 1.5 直接覆盖掉 —— tester 实测 {@code Motion[1] = 0.4}、只升 1.15 格。
     * 近战走 {@code afterMeleeHit}，在击退之后，所以近战 64 格一直正常、不需要延后。
     */
    private static final Map<UUID, PendingLaunch> PENDING = new HashMap<>();

    /** 队列项最多等这么久（tick）；超时（区块卸载 / 实体消失等）直接丢弃，避免泄漏 */
    private static final long PENDING_EXPIRE_TICKS = 20L;

    /** 全局 tick 监听只挂一次（本类可能被多份材料/部件触达） */
    private static boolean tickListenerRegistered = false;

    /** 一条待施加的抛起：维度 + 目标 + 速度 + 过期时间 */
    private record PendingLaunch(ResourceKey<Level> dimension, UUID targetId, double velocity, long expireGameTime) {}

    public AosoraLaunchModifier() {
        // 照项目既有写法用 EVENT_BUS.addListener，和其它词条一样从 modifier 构造期注册；
        // 静态守卫保证只注册一次（本类可能因为多份材料 / 多个箭部件被构造多次）。
        if (!tickListenerRegistered) {
            tickListenerRegistered = true;
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                    TickEvent.LevelTickEvent.class, AosoraLaunchModifier::onLevelTick);
        }
    }

    @Override
    public void onAfterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                LivingEntity attacker, LivingEntity target, float damageDealt) {
        // 没打出伤害（无敌帧 / 免疫 / 被格挡）就不抛
        if (damageDealt <= 0) return;
        if (attacker == null || target == null) return;
        // 服务端权威：客户端不设置速度
        if (target.level().isClientSide()) return;
        if (target == attacker || target.isDeadOrDying()) return;
        // 骑乘中的目标位移由载具接管，硬设速度没意义
        if (target.isPassenger()) return;
        // 只认手里的武器：空手 / 只穿苍彼护甲 / 手持别的武器都不触发
        if (TraitLevels.heldLevel(attacker, AOSORA_TRAIT_ID) <= 0) return;

        launch(target, AOSORA_LAUNCH_VELOCITY);
    }

    /**
     * 箭矢命中：把目标排进「下一 tick 施加」的队列，用远程的 {@link #AOSORA_RANGED_VELOCITY}。
     * <p>
     * <b>不在这里直接写速度</b>：此刻箭矢自己的伤害还没结算，{@code LivingEntity#hurt} 的受击击退
     * 随后会把速度覆盖成 0.4（实测只升 1.15 格）。延后到 {@code LevelTickEvent.END}（同一 tick 末，
     * 已经晚于击退）再写，才是「先挨打、再被挑飞」的正确顺序。
     * <p>
     * 守卫/去重与近战共用：目标已死、骑乘中、正在上升（{@link #RISING_THRESHOLD}）、
     * 或 20 tick 内已经被抬过一次（{@link #LAST_LAUNCH}）都直接跳过。
     */
    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level,
                                      Projectile projectile, AbstractArrow arrow, EntityHitResult hit,
                                      LivingEntity attacker, LivingEntity target) {
        if (level <= 0 || attacker == null || target == null) return;
        if (target.level().isClientSide()) return;
        if (target == attacker || target.isDeadOrDying()) return;
        if (target.isPassenger()) return;
        queueLaunch(target, AOSORA_RANGED_VELOCITY);
    }

    /**
     * 排队一次延迟抛起（只在服务端调用）。去重记账就发生在这里，
     * 所以穿射箭命中多只怪时每只各排一次、同一目标 20 tick 内只排一次。
     */
    private static void queueLaunch(LivingEntity target, double velocity) {
        if (target.getDeltaMovement().y > RISING_THRESHOLD) return;

        Level level = target.level();
        long gameTime = level.getGameTime();
        UUID uuid = target.getUUID();
        Long last = LAST_LAUNCH.get(uuid);
        if (last != null && gameTime - last < LAUNCH_COOLDOWN_TICKS) return;
        LAST_LAUNCH.put(uuid, gameTime);
        cleanUp(gameTime);

        PENDING.put(uuid, new PendingLaunch(level.dimension(), uuid, velocity, gameTime + PENDING_EXPIRE_TICKS));
    }

    /**
     * 服务端每 tick 末把队列里属于当前维度的项弹出来施加。
     * <p>
     * 清理策略：弹出即从队列移除（每条只应用一次）；过期（超过 {@link #PENDING_EXPIRE_TICKS}，
     * 意味着这中间实体所在的 level 没 tick 过，例如区块卸载）直接丢弃；实体已消失 / 已死 / 骑乘中同样丢弃。
     */
    private static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (PENDING.isEmpty()) return;
        if (!(event.level instanceof ServerLevel level)) return;

        long gameTime = level.getGameTime();
        ResourceKey<Level> dimension = level.dimension();
        Iterator<Map.Entry<UUID, PendingLaunch>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            PendingLaunch pending = it.next().getValue();
            if (!dimension.equals(pending.dimension())) continue;
            it.remove();

            if (gameTime > pending.expireGameTime()) continue;
            Entity entity = level.getEntity(pending.targetId());
            if (!(entity instanceof LivingEntity target)) continue;
            if (target.isDeadOrDying() || target.isPassenger()) continue;
            applyLaunch(target, pending.velocity());
        }
    }

    /**
     * 给目标一次向上的冲量。全局只有这一次速度写入，不做逐 tick 的推力，
     * 之后完全交给原版重力 + 空气阻尼自然下坠。
     * <p>
     * 近战分支（{@link #onAfterMeleeHit}）仍然<b>同步</b>调用这里：近战钩子在受击击退之后执行，
     * 时序本来就没问题，改成排队反而慢 1 tick。
     */
    private static void launch(LivingEntity target, double velocity) {
        // 已经在上升途中就不重复叠加
        if (target.getDeltaMovement().y > RISING_THRESHOLD) return;

        long gameTime = target.level().getGameTime();
        UUID uuid = target.getUUID();
        Long last = LAST_LAUNCH.get(uuid);
        if (last != null && gameTime - last < LAUNCH_COOLDOWN_TICKS) return;
        LAST_LAUNCH.put(uuid, gameTime);
        cleanUp(gameTime);

        applyLaunch(target, velocity);
    }

    /** 真正写速度的地方（近战同步调用 / 远程由 LevelTickEvent.END 弹出后调用） */
    private static void applyLaunch(LivingEntity target, double velocity) {
        // 竖直抛起：清掉水平分量，只留向上速度（用户要的是"向上抛起，然后自然下坠"）
        target.setDeltaMovement(0.0D, velocity, 0.0D);
        // 玩家目标的位移由客户端预测、服务端校正，必须打 hurtMarked，
        // ServerEntity 才会在下次 sendChanges 时补一个 ClientboundSetEntityMotionPacket；
        // 生物本身是服务端权威的，普通 setDeltaMovement 就会同步。
        if (target instanceof Player) {
            target.hurtMarked = true;
        }
        // 刻意不清 fallDistance：抛起后要保留自然坠落伤害，不做免摔。
    }

    /** 定期清掉过期的去重记录（同一次运行里不会无限增长）。 */
    private static void cleanUp(long gameTime) {
        if (LAST_LAUNCH.size() <= LAST_LAUNCH_CLEANUP_SIZE) return;
        Iterator<Map.Entry<UUID, Long>> it = LAST_LAUNCH.entrySet().iterator();
        while (it.hasNext()) {
            if (gameTime - it.next().getValue() >= LAUNCH_COOLDOWN_TICKS) {
                it.remove();
            }
        }
    }
}
