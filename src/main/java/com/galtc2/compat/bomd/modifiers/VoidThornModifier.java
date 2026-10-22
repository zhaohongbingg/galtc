package com.galtc2.compat.bomd.modifiers;

import com.cerbon.bosses_of_mass_destruction.particle.BMDParticles;
import com.cerbon.cerbons_api.api.general.event.EventScheduler;
import com.cerbon.cerbons_api.api.general.event.TimedEvent;
import com.cerbon.cerbons_api.api.static_utilities.MathUtils;
import com.cerbon.cerbons_api.api.static_utilities.ParticleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.ArrayList;
import java.util.List;

/**
 * 永恒少女 (void_thorn) —— 祸乱鬼魅 联动强化。
 * <p>
 * 近战命中后，围着目标撒一圈地刺，每级多撒一圈。延迟 {@value #RIFT_TIME} tick 才扎出来，
 * 目标有半秒能躲。
 * <ul>
 *   <li>伤害 = 施法者当前攻击力（玩家身上已含手里匠魂工具的加成）；</li>
 *   <li>伤害源是 {@code galtc2:thorn}（见 {@link BomdHelper#thornHit}），
 *       带 {@code bypasses_cooldown}，<b>不会被目标的无敌帧吃掉</b>；</li>
 *   <li>刺就落在地面那一格，<b>不往上窜</b> —— 一格一根、各判各的。</li>
 * </ul>
 * <b>为什么不用 BOMD 的 {@code RiftBurst}</b>：它的柱子高度是硬编码的
 * （{@code TimedEvent(..., 0, 7)} + {@code ticks.addAndGet(2)}，也就是 0/2/4/6/8/10/12 格共 7 段），
 * 而判定只取 {@code new AABB(columnPos)} 这一格 —— 上面 6 段纯装饰、不结算任何伤害，
 * 看起来就像多冒出来"第二段"。这里改成自己按同一套落点规则（{@link BomdHelper#findSpikePos}，
 * 照 BOMD 的 {@code RiftBurst.defaultPosFinder} 抄的）在地面逐格放置。
 * <p>
 * <b>为什么不用 {@code void_blossom.Spikes}</b>：那个类自己只画预警粒子，真正的柱状尖刺是
 * {@code SpikeAction} 额外发 {@code SpikeS2CPacket}、由绑在 {@code VoidBlossomEntity} 上的
 * 客户端渲染器画的 —— 玩家不是那个实体，客户端拿不到渲染器。
 */
public class VoidThornModifier extends Modifier implements MeleeHitModifierHook {

    /** 预警到真正冒刺的延迟（tick）；BOMD 用 20 */
    private static final int RIFT_TIME = 20;
    /** 撒点用的圆环半径（格）；BOMD 的 SpikeAction 用 2.0 */
    private static final double CIRCLE_RADIUS = 2.0;
    /** 圆盘上的落点（水平偏移），只算一次 */
    private static final List<Vec3> CIRCLE_POINTS = MathUtils.buildBlockCircle(CIRCLE_RADIUS);

    private static final Logger LOGGER = LoggerFactory.getLogger("galtc2/void_thorn");
    /** 实体持久数据里记录「上次触发所在的游戏刻」，用来做去重 */
    private static final String KEY_LAST_TICK = "galtc2_void_thorn_tick";
    /**
     * 去重窗口（tick）。
     * <p>
     * 带「横扫 / 环形攻击」的武器会在同一刻对范围内每个目标各走一遍 {@code MELEE_HIT}，
     * 于是同一片位置会冒出两组刺。留 2 tick 的窗口足够吃掉这种重复，
     * 又远小于最快武器的攻击间隔（≥4 tick），不会影响正常连击。
     */
    private static final int THROTTLE_TICKS = 2;

    /** 预警粒子：地面上的一圈标记（BOMD 给虚空之花用的就是它） */
    private static final RegistryObject<SimpleParticleType> INDICATOR =
            BMDParticles.VOID_BLOSSOM_SPIKE_INDICATOR;

    /**
     * 真正冒出来的刺。
     * <p>
     * BOMD 没给虚空之花配「实体刺」粒子（它的尖刺是客户端模型渲染的），所以借黑曜巨石那一支的
     * 单帧尖刺粒子 {@code OBSIDILITH_SPIKE}（贴图 {@code minecraft:critical_hit}）。
     * 觉得不合适的话换成 {@code BMDParticles.OBSIDILITH_SPIKE_INDICATOR} 即可。
     */
    private static final RegistryObject<SimpleParticleType> SPIKE =
            BMDParticles.OBSIDILITH_SPIKE;

    public VoidThornModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker == null || target == null || !target.isAlive()) {
            return;
        }
        // 额外攻击（横扫之类）不补刺，否则一次挥砍会在每个被扫到的目标脚下都来一圈
        if (context.isExtraAttack()) {
            return;
        }
        // 再去重一次：武器定义里挂了多个攻击模块时，同一刻会对同一目标重复走 MELEE_HIT。
        // 记录写在**实体**的持久数据上而不是工具 NBT —— 工具视图在攻击流程里可能被重建，
        // 写工具 NBT 不保证下一次调用还能读到。
        int now = (int) level.getGameTime();
        CompoundTag data = attacker.getPersistentData();
        int last = data.getInt(KEY_LAST_TICK);
        if (now - last >= 0 && now - last <= THROTTLE_TICKS) {
            LOGGER.info("[永恒少女] 挡掉一次重复触发：now={} last={} target={}",
                    now, last, target.getName().getString());
            return;
        }
        data.putInt(KEY_LAST_TICK, now);

        // 地刺靠 cerbons_api 的事件调度器延时结算，拿不到就直接跳过
        EventScheduler scheduler = BomdHelper.levelScheduler(level);
        if (scheduler == null) {
            return;
        }

        // 落点：围着目标撒一圈（每级多一圈），地面高度由服务端自己算
        List<BlockPos> placements = new ArrayList<>();
        Vec3 center = target.position();
        for (int round = 0; round < modifier.getLevel(); round++) {
            for (Vec3 offset : CIRCLE_POINTS) {
                BlockPos pos = BomdHelper.findSpikePos(level, center.add(offset));
                if (pos != null) {
                    placements.add(pos);
                }
            }
        }
        if (placements.isEmpty()) {
            LOGGER.info("[永恒少女] 触发 tick={} 等级={}，但没找到可用落点", now, modifier.getLevel());
            return;
        }
        LOGGER.info("[永恒少女] 触发 tick={} 等级={} 落点={}", now, modifier.getLevel(), placements.size());

        // 预警：先在地面点出标记，目标有 RIFT_TIME tick 能躲
        for (BlockPos pos : placements) {
            ParticleUtils.spawnParticle(level, INDICATOR.get(), spot(pos, 0.02), Vec3.ZERO, 0, 0.0);
        }

        // 到点：原地冒一根刺，并结算落在那一格里的活体
        scheduler.addEvent(new TimedEvent(() -> {
            for (BlockPos pos : placements) {
                ParticleUtils.spawnParticle(level, SPIKE.get(), spot(pos, 0.2), Vec3.ZERO, 0, 0.0);
            }
            for (BlockPos pos : placements) {
                AABB box = new AABB(pos).inflate(0.1);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != attacker && e.isAlive())) {
                    BomdHelper.thornHit(level, attacker, victim);
                }
            }
        }, RIFT_TIME, 1, () -> !attacker.isAlive()));
    }

    /** 落点方块上表面略偏一点的位置，用来撒粒子 */
    private static Vec3 spot(BlockPos pos, double y) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + y, pos.getZ() + 0.5);
    }
}
