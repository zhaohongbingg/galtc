package com.galtc2.compat.bomd.modifiers;

import com.cerbon.bosses_of_mass_destruction.entity.custom.obsidilith.RiftBurst;
import com.cerbon.bosses_of_mass_destruction.particle.BMDParticles;
import com.cerbon.cerbons_api.api.general.event.EventScheduler;
import com.cerbon.cerbons_api.api.static_utilities.MathUtils;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * 宝石少女 (obsidian_heart) —— 祸乱鬼魅 联动强化。
 * <p>
 * 近战命中后，围着目标撒一圈 BOMD「黑曜巨石」的地刺（{@link RiftBurst}），每级多撒一圈。
 * <p>
 * BOMD 的黑曜巨石一共做了<b>三套</b>地刺粒子（分别用在 {@code SpikeAction} /
 * {@code BurstAction} / {@code WaveAction} 里），外观是同一套东西换贴图，见
 * {@link #SPIKE_STYLES}。这里每次触发随机挑一套，所以就出现了"柱子 / 爆发 / 波纹"三种随机切换。
 * <p>
 * 结算照 BOMD 的 {@code obsidilith.SpikeAction}：
 * <ul>
 *   <li>伤害 = 施法者当前攻击力（玩家身上已含手里匠魂工具的加成），
 *       伤害源走 {@code BMDUtils.shieldPiercing}，<b>无视护盾</b>；</li>
 *   <li>命中额外附加缓慢（照 BOMD，120 tick / 等级 II）。</li>
 * </ul>
 * 自研方块 {@code galtc2:obsidian_spike} 已停用（BOMD 这套本来就是"粒子 + 延迟伤害"而不是真方块）。
 */
public class ObsidianHeartModifier extends Modifier implements MeleeHitModifierHook {

    /** 预警到真正冒刺的延迟（tick）；BOMD 用 20 */
    private static final int RIFT_TIME = 20;
    /** 撒点用的圆环半径（格）；BOMD 的 SpikeAction 用 2.0 */
    private static final double CIRCLE_RADIUS = 2.0;
    /** 圆环上的落点（水平偏移），只算一次 */
    private static final List<Vec3> CIRCLE_POINTS = MathUtils.buildBlockCircle(CIRCLE_RADIUS);
    /** 扎中后附加的缓慢时长（tick）与等级，照 BOMD 的黑曜巨石 */
    private static final int SLOWNESS_DURATION = 120;
    private static final int SLOWNESS_AMPLIFIER = 2;
    /** 实体持久数据里记录「上次触发所在的游戏刻」，用来做去重 */
    private static final String KEY_LAST_TICK = "galtc2_obsidian_heart_tick";
    /** 去重窗口（tick）；理由见 {@link VoidThornModifier} 里的同一段说明 */
    private static final int THROTTLE_TICKS = 2;

    /**
     * 黑曜巨石的三套地刺粒子，随机切换。
     * <p>
     * 三套分别对应 BOMD 的 {@code SpikeAction} / {@code BurstAction} / {@code WaveAction}，
     * 参数（预警粒子 + 柱状粒子）与它们一一对应。
     */
    private static final List<SpikeStyle> SPIKE_STYLES = List.of(
            new SpikeStyle(BMDParticles.OBSIDILITH_SPIKE_INDICATOR, BMDParticles.OBSIDILITH_SPIKE),
            new SpikeStyle(BMDParticles.OBSIDILITH_BURST_INDICATOR, BMDParticles.OBSIDILITH_BURST),
            new SpikeStyle(BMDParticles.OBSIDILITH_WAVE_INDICATOR, BMDParticles.OBSIDILITH_WAVE));

    /** 一套地刺粒子：预警 + 柱状 */
    private record SpikeStyle(RegistryObject<SimpleParticleType> indicator,
                              RegistryObject<SimpleParticleType> column) {
    }

    public ObsidianHeartModifier() {
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
        // 再去重一次：写**实体**的持久数据（理由见 VoidThornModifier —— 工具视图可能被重建）
        int now = (int) level.getGameTime();
        CompoundTag data = attacker.getPersistentData();
        int last = data.getInt(KEY_LAST_TICK);
        if (now - last >= 0 && now - last <= THROTTLE_TICKS) {
            return;
        }
        data.putInt(KEY_LAST_TICK, now);

        // 地刺靠 cerbons_api 的事件调度器延时结算，拿不到就直接跳过
        EventScheduler scheduler = BomdHelper.levelScheduler(level);
        if (scheduler == null) {
            return;
        }

        // 三套外观随机挑一套
        SpikeStyle style = SPIKE_STYLES.get(level.getRandom().nextInt(SPIKE_STYLES.size()));

        RiftBurst burst = new RiftBurst(attacker, level,
                style.indicator().get(),
                style.column().get(),
                RIFT_TIME, scheduler,
                victim -> {
                    BomdHelper.thornHit(level, attacker, victim);
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            SLOWNESS_DURATION, SLOWNESS_AMPLIFIER));
                });

        Vec3 center = target.position();
        for (int round = 0; round < modifier.getLevel(); round++) {
            for (Vec3 offset : CIRCLE_POINTS) {
                burst.tryPlaceRift(center.add(offset));
            }
        }
    }
}
