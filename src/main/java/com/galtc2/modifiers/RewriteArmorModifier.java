package com.galtc2.modifiers;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 改写（护甲侧）—— 罚抄材料的防御版特性。
 * <p>
 * 工具侧 {@link RewriteModifier} 是「命中时把目标的护甲与生命记下来加到自己身上」；
 * 护甲侧反过来：<b>被打时读攻击者当前的护甲 / 坚韧 / 生命上限，把这份数值「复写」一份加到自己身上</b>，
 * 各 {@value #DURATION} tick（5 秒）后自己这边还原。
 * <p>
 * <b>绝不修改攻击者</b>（用户 2026-10-26 明确要求：「罚抄不修改对面的生物，避免 bug」）：
 * 本类对 {@code attacker} 只做<b>读</b>（{@code getAttributeValue}），没有任何
 * {@code addTransientModifier} / {@code setHealth} / {@code removeModifier} 落到攻击者身上。
 * <p>
 * 注意这条走的是 {@code ON_ATTACKED} 钩子（护甲被攻击时触发），而不是 {@code INVENTORY_TICK} ——
 * 后者只对玩家背包里的物品调用，女仆/怪物穿护甲时不会 tick，所以护甲类词条不能靠它。
 *
 * <h2>两套独立计时（关键）</h2>
 * <b>护甲 / 坚韧</b>与<b>生命上限（改血）</b>是<b>两套互不相干的计时</b>：
 * <ul>
 *   <li>{@link #ARMOR_ACTIVE}：{@code ArmorSteal(attackerId, armor, toughness, expire)}。
 *       同一个攻击者继续打 → 只续期；换了攻击者 → 先还自己这份再按新攻击者数值复写；
 *       到期/持有者死亡 → {@link #revertArmor}（<b>只摘 ARMOR / ARMOR_TOUGHNESS，不碰生命上限、不 clamp 血量</b>）。</li>
 *   <li>{@link #HEALTH_ACTIVE}：{@code HealthSteal(amount, expire)}，<b>同一时间只允许一份生效</b>。
 *       生效期内（{@value #DURATION} tick）无论被打多少次、攻击者是否更换，<b>一律不改血、不续期、不补血</b>；
 *       只有为空或已过期时才允许下一次改血（加 {@code SELF_HEALTH_UUID} 上限 + 补等量血 + 写表）。
 *       到期/持有者死亡 → {@link #revertHealth}（摘 {@code SELF_HEALTH_UUID} 并 {@code clampHealth}）。</li>
 * </ul>
 *
 * <h2>为什么改血必须单独加 CD</h2>
 * F-009 的 bug（用户反馈「我怎么感觉我血越被打越多」，Lead 用日志+存档核实）：
 * 一次会话 {@code rewrite_armor: steal} 打了 <b>104 次</b>，攻击者在僵尸 / 骷髅之间来回换 →
 * 每次换攻击者都「还原 + 重新复写」，而每次复写都 {@code holder.setHealth(hp + stolen)}（上限 +20 且立刻补 20 血），
 * 边挨打边回血；更糟的是 transient 的生命上限不落盘、补出来的血却落盘了，
 * 存档里出现 {@code Health 37.12} 而 base max_health 只有 20（下次进游戏被原版 setHealth 夹回，表现为「突然掉血」）。
 * 现在按用户要求（「设置 cd 就行，上一个改血结束前，不进行下一个改血」）：
 * 改血频率被硬性限制在 <b>≤ 1 次 / 5 秒</b>，且护甲侧的还原再也不会连带摘掉生命上限。
 *
 * <h2>状态机（每次命中只复写一次，不是每件甲一次）</h2>
 * {@code ON_ATTACKED} 是<b>每件护甲各派发一次</b>的（TConstruct 对 4 个 armor slot + 主副手 HELD 各调一次），
 * 穿 4 件就是 4~6 次回调。所以复写量不能写在回调里，而是记在静态表里（key = 持有者 UUID）：
 * 同一帧里第 1 次真的复写，后面几次只会走「续期」，不会叠 4 倍。
 * <p>
 * 回收挂在全局 {@link LivingEvent.LivingTickEvent} 上（构造函数里注册一次，仿 {@code OneHeartModifier}），
 * 但<b>先按 UUID 查表</b>，表里没有的实体（99.9%）只走两次 map 查找就返回，不做遍历。
 *
 * <h2>旧存档脏数据清理</h2>
 * 旧版（0.x）在玩家身上留下了 3 个 permanent modifier，无法靠新逻辑的 UUID 摘掉，
 * 所以保留 {@link #LEGACY_*_UUID} 三个旧值，在 tick 里发现实体 persistent data 还带着
 * {@code galtc2:rewrite_armor_expire} 就摘掉它们、clamp 血量、删键（只做一次）。
 * 新逻辑自己的 3 个 UUID 与旧值完全不同，互不干扰。
 *
 * <h2>已知取舍</h2>
 * <ul>
 *   <li>没有「对方被你偷掉护甲」的压制感了，只剩自己 5 秒强化（用户主动选择，换掉 PvP 边缘 bug）；</li>
 *   <li>换攻击者只影响自己这份护甲/坚韧的复写量重算，<b>不影响</b>改血 CD；</li>
 *   <li>transient 的生命上限不落盘：若玩家在加成期间退出游戏，存档里的血量会被原版夹回 base 上限（一次性掉血）。</li>
 * </ul>
 *
 * <h2>tester 期望观测量（游戏内验证用）</h2>
 * <ol>
 *   <li><b>攻击者全程不变（F-009 判据）</b>：两只僵尸的 {@code armor / armor_toughness / max_health / Health}
 *       读数一字不变。</li>
 *   <li><b>第一次命中</b>：{@code max_health} 20 → 40、{@code Health} 补到 40（+复写量，封顶 20）；
 *       日志 {@code steal … healthApplied=true}。</li>
 *   <li><b>生效期 1–5 秒内</b>：{@code Health} <b>只减不增</b>（只掉所受伤害），{@code max_health} 恒 40；
 *       换攻击者、护甲侧还原都不会补血；这一段的日志全是 {@code healthApplied=false}。</li>
 *   <li><b>第 5 秒后</b>：{@code max_health} 回 20、{@code Health} clamp（≤20）；日志 {@code revertHealth holderFound=true amount=…}。</li>
 *   <li><b>再过 5 秒再被打</b>：{@code max_health} 又到 40、血量再补一次 —— 改血频率 ≤ 1 次 / 5 秒。</li>
 *   <li><b>不触发</b>：{@code /damage @s 10 generic}（无实体来源）、自伤（attacker == holder）、脱甲、
 *       {@code mob_attack} 不写 {@code by} → 双方都不变，也不写 persistent data。</li>
 *   <li><b>取证方式</b>：{@code /attribute <target> minecraft:generic.<attr> get}（transient modifier 不进存档 NBT，
 *       {@code /data get entity … Attributes} 看不到）；血量用 {@code /data get entity @s Health}。</li>
 * </ol>
 */
public class RewriteArmorModifier extends BaseModifiers {

    private static final Logger LOGGER = LogUtils.getLogger();

    /* ============================ 旧存档（0.x）遗留 UUID：只用于清理，不再写入 ============================ */

    /** 旧版护甲加成 UUID（permanent，用户存档里挂着 +6） */
    private static final UUID LEGACY_ARMOR_UUID = UUID.fromString("a1b2c3d4-e5f6-4708-9a0b-1c2d3e4f5061");
    /** 旧版生命上限加成 UUID（permanent，+4） */
    private static final UUID LEGACY_HEALTH_UUID = UUID.fromString("b2c3d4e5-f607-4819-ab0c-2d3e4f506172");
    /** 旧版坚韧加成 UUID（permanent，+4） */
    private static final UUID LEGACY_TOUGHNESS_UUID = UUID.fromString("c3d4e5f6-0718-492a-bc0d-3e4f50617283");
    /** 旧版写在实体 persistent data 里的到期书签（新逻辑不再写这个键） */
    private static final String LEGACY_EXPIRE_KEY = "galtc2:rewrite_armor_expire";

    /* ============================ 新逻辑 UUID：只加在持有者自己身上（正向） ============================ */

    private static final UUID SELF_ARMOR_UUID = UUID.fromString("9f1e0d2c-3b4a-4c5d-8e6f-0a1b2c3d4e01");
    private static final UUID SELF_TOUGHNESS_UUID = UUID.fromString("9f1e0d2c-3b4a-4c5d-8e6f-0a1b2c3d4e02");
    private static final UUID SELF_HEALTH_UUID = UUID.fromString("9f1e0d2c-3b4a-4c5d-8e6f-0a1b2c3d4e03");

    private static final String SELF_ARMOR_NAME = "rewrite_armor_steal";
    private static final String SELF_TOUGHNESS_NAME = "rewrite_toughness_steal";
    private static final String SELF_HEALTH_NAME = "rewrite_health_steal";

    /* ============================ 数值上限 / 时长 ============================ */

    /**
     * 单次复写的护甲上限：护甲 20 已经是下界合金全套 + 附魔的水平，再多没有意义；
     * 同时挡住「复制凋灵 / 混沌怪几百点护甲」这种破坏平衡的情况。
     */
    private static final double MAX_STEAL_ARMOR = 20.0D;
    /** 单次复写的坚韧上限（全套下界合金坚韧 12，取 10） */
    private static final double MAX_STEAL_TOUGHNESS = 10.0D;
    /**
     * 单次复写的生命上限：卡片要求 ≤20，避免抄凋灵（300 血）直接变成大血牛。
     * <p>只是「加到自己身上多少」的上限，攻击者<b>不被修改</b>，所以不需要给对方留任何保底血量。
     */
    private static final double MAX_STEAL_HEALTH = 20.0D;

    /** 加成持续时间（tick）：100 tick = 5 秒。护甲/坚韧与改血各自按这个时长独立计时 */
    private static final int DURATION = 100;

    /** 旧存档清理扫描间隔（tick）：1 秒扫一次，命中即删键，之后不再进入 */
    private static final int LEGACY_SWEEP_INTERVAL = 20;

    /** 兜底到期扫描间隔（tick）：持有者下线/区块卸载时它的 tick 不跑，靠场上任意实体兜底 */
    private static final int EXPIRE_SWEEP_INTERVAL = 20;

    /**
     * 护甲 / 坚韧的复写状态：持有者 UUID → 本次复写的量与到期 tick。
     * <p>放在静态表里而不是 persistent data，是为了不产生任何存档脏数据；
     * 服务端单线程 tick，用 ConcurrentHashMap 只是防御性的。
     * <p>{@code attackerId} 只用来判断「是不是同一个攻击者」（续期 or 重新复写），
     * 不用于任何属性写入 —— 攻击者本身不被修改。
     */
    private static final Map<UUID, ArmorSteal> ARMOR_ACTIVE = new ConcurrentHashMap<>();

    /**
     * 改血（生命上限）的状态：与 {@link #ARMOR_ACTIVE} <b>完全独立</b>的计时。
     * <p>有记录且未过期 = 改血 CD 生效中：这段时间里任何命中都<b>不改血、不续期、不补血</b>。
     * 这是修复「血越被打越多」（用户要求「设置 cd」）的核心：改血频率被限制在 ≤ 1 次 / 5 秒。
     */
    private static final Map<UUID, HealthSteal> HEALTH_ACTIVE = new ConcurrentHashMap<>();

    /** 一份护甲 / 坚韧的复写状态 */
    private record ArmorSteal(UUID attackerId, double armor, double toughness, long expireGameTime) {
        /** 同一个攻击者继续命中时只续期，复写量不变 */
        ArmorSteal renew(long expire) {
            return new ArmorSteal(this.attackerId, this.armor, this.toughness, expire);
        }
    }

    /** 一份改血状态：这次加了多少生命上限，什么时候还原 */
    private record HealthSteal(double amount, long expireGameTime) {
    }

    public RewriteArmorModifier() {
        // 全局监听只注册一次：本词条由 galtc2Modifier 用 StaticModifier 单例构造（见 Register/galtc2Modifier#rewrite_armor）
        MinecraftForge.EVENT_BUS.addListener(this::onLivingTick);
    }

    @Override
    public boolean isNoLevels() {
        return true;
    }

    /* ================================================================ 被打：复写一份给自己 */

    @Override
    public void modifierOnAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                                   EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity holder = context.getEntity();
        if (holder == null || holder.level().isClientSide()) {
            return;
        }
        // 无实体来源（/damage @s 10 generic、摔落、火、饥饿…）没有可读的数值 → 不触发
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker == holder) {
            return;
        }
        if (attacker.isDeadOrDying() || holder.isDeadOrDying()) {
            return;
        }
        if (!attacker.level().dimension().equals(holder.level().dimension())) {
            return;
        }
        // 注意：下面只读 attacker 的属性，不写（用户要求「罚抄不修改对面的生物」）
        steal(holder, attacker, holder.level().getGameTime());
    }

    /**
     * 把攻击者的护甲 / 坚韧 / 生命上限复写一份加到自己身上。
     * <p>护甲/坚韧走「同攻击者续期、换攻击者先还原再复写」；<b>改血单独走 CD</b>，两者互不牵连。
     */
    private static void steal(LivingEntity holder, LivingEntity attacker, long gameTime) {
        UUID holderId = holder.getUUID();
        UUID attackerId = attacker.getUUID();

        /* ---------- 1) 护甲 / 坚韧：每次命中都可刷新，换攻击者先还原自己这份 ---------- */
        boolean renewed = false;
        ArmorSteal previous = ARMOR_ACTIVE.get(holderId);
        if (previous != null) {
            if (previous.attackerId().equals(attackerId) && isArmorApplied(holder, previous)) {
                // 同一个攻击者继续打：只续期，绝不重复叠加
                ARMOR_ACTIVE.put(holderId, previous.renew(gameTime + DURATION));
                renewed = true;
            } else {
                // 换了攻击者，或者表里有记录但自己身上的 modifier 已经不在了（重生/被其它模组清属性）：
                // 先还自己这份（不会连带摘生命上限），再按当前攻击者的数值复写
                revertArmor(holderId, previous, holder);
            }
        }

        // 只读攻击者数值：攻击者身上一个 modifier 都不会被本词条加上
        double armor = cap(attacker.getAttributeValue(Attributes.ARMOR), 0.0D, MAX_STEAL_ARMOR);
        double toughness = cap(attacker.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 0.0D, MAX_STEAL_TOUGHNESS);

        if (!renewed) {
            if (armor <= 0.0D && toughness <= 0.0D) {
                // 攻击者身上没护甲/坚韧可抄：不建状态，避免空转
                ARMOR_ACTIVE.remove(holderId);
            } else {
                if (armor > 0.0D) {
                    apply(holder, Attributes.ARMOR, SELF_ARMOR_UUID, SELF_ARMOR_NAME, armor);
                }
                if (toughness > 0.0D) {
                    apply(holder, Attributes.ARMOR_TOUGHNESS, SELF_TOUGHNESS_UUID, SELF_TOUGHNESS_NAME, toughness);
                }
                ARMOR_ACTIVE.put(holderId, new ArmorSteal(attackerId, armor, toughness, gameTime + DURATION));
            }
        }

        /* ---------- 2) 改血（生命上限）：独立 CD，生效期内一律不动 ---------- */
        double health = cap(attacker.getAttributeValue(Attributes.MAX_HEALTH), 0.0D, MAX_STEAL_HEALTH);
        boolean healthApplied = false;

        HealthSteal healthSteal = HEALTH_ACTIVE.get(holderId);
        if (healthSteal != null
                && (gameTime >= healthSteal.expireGameTime()
                || !stillHas(holder, Attributes.MAX_HEALTH, SELF_HEALTH_UUID))) {
            // 已过期（tick 还没来得及回收）或上限 modifier 已经不在（重生/被清属性）：
            // 先按 CD 结束处理，把该 clamp 的 clamp 掉，再允许下面这一次改血
            revertHealth(holderId, healthSteal, holder);
            healthSteal = null;
        }

        if (healthSteal == null && health > 0.0D) {
            // CD 就绪：加生命上限 + 补等量血 + 开始计时
            apply(holder, Attributes.MAX_HEALTH, SELF_HEALTH_UUID, SELF_HEALTH_NAME, health);
            holder.setHealth(Math.min(holder.getMaxHealth(), holder.getHealth() + (float) health));
            HEALTH_ACTIVE.put(holderId, new HealthSteal(health, gameTime + DURATION));
            healthApplied = true;
        }
        // 否则 healthSteal != null（CD 生效中）：不改血、不续期、不补血，只留一条 healthApplied=false 的日志

        LOGGER.debug("[galtc2] rewrite_armor: steal holder={} attacker={} armor={} toughness={} health={} "
                        + "healthApplied={} expire={}",
                holder.getName().getString(), attacker.getName().getString(), armor, toughness, health,
                healthApplied, gameTime + DURATION);
    }

    /* ================================================================ 还原（只还原自己，且两套分开） */

    /**
     * 只还原<b>护甲 / 坚韧</b>：摘 {@code SELF_ARMOR_UUID} / {@code SELF_TOUGHNESS_UUID}。
     * <p><b>不摘生命上限、不 clamp 血量</b> —— 生命有自己独立的一份计时（{@link #HEALTH_ACTIVE}），
     * 护甲侧还原（例如换了攻击者）绝不允许连带改血，否则又会变成「边挨打边回血」。
     * <p><b>不碰攻击者</b> —— 本词条从来没有修改过对方。
     * <p>{@code holderHint} 能直接拿到实体时传进来省一次查找；从 tick 里清理时传 null，
     * 按 UUID 在所有已加载维度里找 —— 找不到就算了：modifier 是 transient，实体没了加成本来也跟着没了。
     */
    private static void revertArmor(UUID holderId, ArmorSteal steal, @Nullable LivingEntity holderHint) {
        LivingEntity holder = holderHint != null ? holderHint : findLiving(holderId);
        if (holder != null) {
            if (steal.armor() > 0.0D) {
                remove(holder, Attributes.ARMOR, SELF_ARMOR_UUID);
            }
            if (steal.toughness() > 0.0D) {
                remove(holder, Attributes.ARMOR_TOUGHNESS, SELF_TOUGHNESS_UUID);
            }
        }

        LOGGER.debug("[galtc2] rewrite_armor: revertArmor holderFound={} armor={} toughness={}",
                holder != null, steal.armor(), steal.toughness());
    }

    /**
     * 只还原<b>改血</b>：摘 {@code SELF_HEALTH_UUID} 并 clamp 血量。
     * <p>这是本类里<b>唯一</b>会 clamp 血量的路径（旧 {@code revert} 顺手 clamp 的做法已经拆掉）。
     */
    private static void revertHealth(UUID holderId, HealthSteal health, @Nullable LivingEntity holderHint) {
        LivingEntity holder = holderHint != null ? holderHint : findLiving(holderId);
        if (holder != null) {
            remove(holder, Attributes.MAX_HEALTH, SELF_HEALTH_UUID);
            // 血量上限降回去后当前血量可能超标：clamp（不会把血打成 0）
            clampHealth(holder);
        }

        LOGGER.debug("[galtc2] rewrite_armor: revertHealth holderFound={} amount={}",
                holder != null, health.amount());
    }

    /* ================================================================ tick：回收 + 旧存档清理 */

    private void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        long gameTime = entity.level().getGameTime();

        // 1) 旧存档脏数据清理：每秒扫一次；persistent data 里没有那个键时就是一次 contains，命中后永久失效
        if (gameTime % LEGACY_SWEEP_INTERVAL == 0L) {
            sweepLegacy(entity);
        }

        // 2) 持有者自己的到期回收：两张表各自判到期/死亡，先按 UUID 查表，绝大多数实体查两次就返回。
        //    注意这里不再检查攻击者是否还活着 —— 对方不再被本词条修改，它的死活不影响自己这份 5 秒加成
        UUID id = entity.getUUID();
        boolean handled = false;

        ArmorSteal armorSteal = ARMOR_ACTIVE.get(id);
        if (armorSteal != null) {
            if (entity.isDeadOrDying() || gameTime >= armorSteal.expireGameTime()) {
                revertArmor(id, armorSteal, entity);
                ARMOR_ACTIVE.remove(id);
            }
            handled = true;
        }

        HealthSteal healthSteal = HEALTH_ACTIVE.get(id);
        if (healthSteal != null) {
            if (entity.isDeadOrDying() || gameTime >= healthSteal.expireGameTime()) {
                revertHealth(id, healthSteal, entity);
                HEALTH_ACTIVE.remove(id);
            }
            handled = true;
        }

        if (handled) {
            return;
        }

        // 3) 兜底：持有者下线（玩家）或所在区块卸载（怪物）时它自己的 tick 不跑，
        //    由场上任意实体的 tick 每 20 tick 把两张表各扫一遍，保证加成一定会被收回去。
        if (gameTime % EXPIRE_SWEEP_INTERVAL == 0L) {
            if (!ARMOR_ACTIVE.isEmpty()) {
                ARMOR_ACTIVE.entrySet().removeIf(entry -> {
                    ArmorSteal value = entry.getValue();
                    if (gameTime < value.expireGameTime()) {
                        return false;
                    }
                    revertArmor(entry.getKey(), value, null);
                    return true;
                });
            }
            if (!HEALTH_ACTIVE.isEmpty()) {
                HEALTH_ACTIVE.entrySet().removeIf(entry -> {
                    HealthSteal value = entry.getValue();
                    if (gameTime < value.expireGameTime()) {
                        return false;
                    }
                    revertHealth(entry.getKey(), value, null);
                    return true;
                });
            }
        }
    }

    /**
     * 旧版（0.x）脏数据清理：摘掉三个永久 modifier、clamp 血量、删掉过期书签键。
     * <p>旧版只写了书签从来没有回收，所以用户存档里的 +6/+4/+4 是永久的；
     * 新逻辑的 3 个 UUID 与旧值完全不同，摘旧值不会碰到当前的复写状态。
     */
    private static void sweepLegacy(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(LEGACY_EXPIRE_KEY)) {
            return;
        }
        boolean removed = remove(entity, Attributes.ARMOR, LEGACY_ARMOR_UUID);
        removed |= remove(entity, Attributes.ARMOR_TOUGHNESS, LEGACY_TOUGHNESS_UUID);
        removed |= remove(entity, Attributes.MAX_HEALTH, LEGACY_HEALTH_UUID);
        clampHealth(entity);
        // CompoundTag#remove 在 1.20.1 返回 void；键删掉后这条路径对该实体永久失效（只做一次）
        data.remove(LEGACY_EXPIRE_KEY);
        if (removed) {
            LOGGER.debug("[galtc2] rewrite_armor: cleared legacy stat bonuses on {}", entity.getName().getString());
        }
    }

    /* ================================================================ 属性工具方法（只作用于传入的实体） */

    /**
     * 加一个 transient modifier。
     * <p><b>先 {@code removeModifier(uuid)} 再 add</b>：同一个 UUID 重复 add 时
     * {@code AttributeInstance} 会抛 {@code IllegalArgumentException}，
     * 而续期/换攻击者/旧值残留都可能让 UUID 已经存在。
     * <p>调用方只在<b>持有者自己</b>身上用它（攻击者侧没有任何写入，见类注释）。
     */
    private static void apply(LivingEntity entity, Attribute attribute, UUID uuid, String name, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(uuid);
        instance.addTransientModifier(new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADDITION));
    }

    /** 摘掉指定 UUID 的 modifier，存在过就返回 true */
    private static boolean remove(LivingEntity entity, Attribute attribute, UUID uuid) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null || instance.getModifier(uuid) == null) {
            return false;
        }
        instance.removeModifier(uuid);
        return true;
    }

    /** 当前血量超过上限时 clamp（上限被降下来后必须做，否则会留着超标的血量） */
    private static void clampHealth(LivingEntity entity) {
        float max = entity.getMaxHealth();
        if (entity.getHealth() > max) {
            entity.setHealth(max);
        }
    }

    /**
     * 护甲/坚韧这份复写状态在持有者身上是否还真的挂着（玩家死亡重生 = 新实体、{@code /attribute} 清过属性、
     * 其它模组重置过属性都会让它消失）。
     * <p>只看护甲与坚韧：生命上限由 {@link #HEALTH_ACTIVE} 单独把关，不能因为生命 modifier 掉了
     * 就把护甲侧也判成「没生效」（那会变成每挨一下都重新复写护甲）。
     * <p>属性实例读不到时按「还挂着」处理：那样只会走续期，不会陷入「还原 → 立刻再复写」的循环。
     */
    private static boolean isArmorApplied(LivingEntity holder, ArmorSteal steal) {
        return (steal.armor() <= 0.0D || stillHas(holder, Attributes.ARMOR, SELF_ARMOR_UUID))
                && (steal.toughness() <= 0.0D || stillHas(holder, Attributes.ARMOR_TOUGHNESS, SELF_TOUGHNESS_UUID));
    }

    private static boolean stillHas(LivingEntity entity, Attribute attribute, UUID uuid) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null || instance.getModifier(uuid) != null;
    }

    /** 在已加载的维度里按 UUID 找活着的实体；找不到（下线/未加载/已移除）返回 null */
    @Nullable
    private static LivingEntity findLiving(UUID id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private static double cap(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
