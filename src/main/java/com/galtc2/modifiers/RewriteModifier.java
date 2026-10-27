package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 改写：命中时记下目标的护甲与最大生命，并加到自己身上。
 * 每个目标的加成独立计时，满 60 秒或该目标死亡时失效。
 * <p>
 * 加成用 <b>transient</b> modifier（{@code addTransientModifier}）写在攻击者自己的
 * {@code Attributes.ARMOR} / {@code Attributes.MAX_HEALTH} 上，不落盘：
 * 「偷了谁、什么时候到期」只记在内存表 {@link #STOLEN} 里，如果属性本身写进存档，
 * 服务端重启后表清了、永久加成还在，就再也回收不掉（这正是本类之前被报的 bug）。
 * <p>
 * 与星旅（starry_travel）不冲突：星旅武器侧 {@code StarryTravelModifier} 走
 * {@code MELEE_DAMAGE} 钩子直接放大本次攻击伤害（不写任何属性、没有 UUID），
 * 本类走 {@code MELEE_HIT} + 攻击者的护甲 / 生命上限属性（UUID 见下），
 * 两者钩子、作用对象、UUID 全不同，同一件武器上可以同时生效、互不覆盖。
 */
public class RewriteModifier extends Modifier implements MeleeHitModifierHook {

    private static final UUID ARMOR_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
    private static final UUID HEALTH_UUID = UUID.fromString("d4e5f6a7-b8c9-0123-defa-234567890123");
    private static final long DURATION_TICKS = 1200; // 60秒

    /** 攻击者 -> (目标 -> 从该目标偷到的加成)。目标死亡或满 60 秒后移除条目，表空时连攻击者键一起删 */
    private static final Map<UUID, Map<UUID, Steal>> STOLEN = new ConcurrentHashMap<>();
    /** 需要重算属性的攻击者：目标死亡是异步清理，攻击者实体不在手边，只能打标记等它自己的 tick 刷新 */
    private static final Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();

    /** 单个目标贡献的加成 */
    private record Steal(long timestamp, double armor, double health) {}

    public RewriteModifier() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        if (attacker == null || attacker.level().isClientSide()) return;
        LivingEntity target = context.getLivingTarget();
        // 伤害结算在 afterMeleeHit 之前，这一击可能已经把目标打死了：
        // 那时 LivingDeathEvent 早就触发过，现在再写进去就没有任何事件能清掉它（加成会挂在尸体上满 60 秒）。
        if (target == null || target.isDeadOrDying()) return;

        UUID attackerId = attacker.getUUID();
        STOLEN.computeIfAbsent(attackerId, k -> new ConcurrentHashMap<>())
                .put(target.getUUID(), new Steal(
                        attacker.level().getGameTime(),
                        target.getAttributeValue(Attributes.ARMOR),
                        target.getMaxHealth()));
        DIRTY.remove(attackerId);
        updateBonus(attacker);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        UUID targetId = event.getEntity().getUUID();
        STOLEN.forEach((attackerId, targets) -> {
            if (targets.remove(targetId) != null) {
                // 这里拿不到攻击者实体，标记为待刷新，攻击者下一次 tick 时重算（最多晚 1 tick）
                DIRTY.add(attackerId);
            }
        });
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        UUID entityId = entity.getUUID();

        // 旧存档兜底：0.x 版本用 addPermanentModifier 把 rewrite_armor / rewrite_health 写进了存档，
        // 改成 transient 之后这些旧值不会自己消失（当时只有"再拿武器命中一次"才会被 updateBonus 摘掉）。
        // 没有任何活跃偷取状态时身上还挂着这两个 UUID，就只可能是旧版残留 → 直接摘掉并 clamp 血量。
        // 必须放在下面 stolen == null 的早退之前，否则永远走不到这一段。
        // 只对 Player 做：旧版只可能把永久加成写到玩家身上。
        if (entity instanceof Player && !STOLEN.containsKey(entityId) && clearLegacyBonus(entity)) {
            clampHealth(entity);
        }

        Map<UUID, Steal> stolen = STOLEN.get(entityId);
        if (stolen == null) return;

        long currentTime = entity.level().getGameTime();
        boolean changed = stolen.entrySet().removeIf(entry -> currentTime - entry.getValue().timestamp() > DURATION_TICKS);

        if (stolen.isEmpty()) {
            // 全部过期：把攻击者键一起删掉，之后就不再进入这个方法
            STOLEN.remove(entityId, stolen);
            DIRTY.remove(entityId);
            updateBonus(entity);
        } else if (changed || DIRTY.remove(entityId) || !hasBonus(entity)) {
            // 只在真的有变化（过期 / 目标死亡）或属性被外部清掉时才重算，不再每 tick 重加一遍
            updateBonus(entity);
        }
    }

    /**
     * 摘掉身上挂着的 {@link #ARMOR_UUID} / {@link #HEALTH_UUID}（只摘确实存在的那一个）。
     * <p>
     * 只由 {@link #onLivingTick} 的旧存档兜底调用，且调用前已确认该实体<b>没有</b>活跃偷取状态
     * （{@code !STOLEN.containsKey(uuid)}），所以不会误摘正常生效中的加成。
     *
     * @return 至少摘掉了一个才返回 true（调用方据此 clamp 血量）
     */
    private static boolean clearLegacyBonus(LivingEntity entity) {
        boolean removed = removeIfPresent(entity, Attributes.ARMOR, ARMOR_UUID);
        removed |= removeIfPresent(entity, Attributes.MAX_HEALTH, HEALTH_UUID);
        return removed;
    }

    /** 指定属性上确实挂着这个 UUID 的 modifier 时才摘（{@code getModifier} 判空后再动） */
    private static boolean removeIfPresent(LivingEntity entity, Attribute attribute, UUID uuid) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null || instance.getModifier(uuid) == null) {
            return false;
        }
        instance.removeModifier(uuid);
        return true;
    }

    /** 生命上限被摘掉后把当前血量 clamp 回上限（旧版 +生命上限会让存档里的血量停在旧上限附近） */
    private static void clampHealth(LivingEntity entity) {
        float max = entity.getMaxHealth();
        if (entity.getHealth() > max) {
            entity.setHealth(max);
        }
    }

    /** 攻击者身上是否还挂着本次的加成（死亡重生、被其它东西清掉属性时需要补回） */
    private static boolean hasBonus(LivingEntity attacker) {
        AttributeInstance armorAttr = attacker.getAttribute(Attributes.ARMOR);
        AttributeInstance healthAttr = attacker.getAttribute(Attributes.MAX_HEALTH);
        return (armorAttr == null || armorAttr.getModifier(ARMOR_UUID) != null)
                && (healthAttr == null || healthAttr.getModifier(HEALTH_UUID) != null);
    }

    /**
     * 重算并写入攻击者身上的加成（唯一写属性的地方）。
     * <p>
     * <b>用 transient 而不是 permanent</b>：{@code addPermanentModifier} 的属性会写进实体存档，
     * 而「偷了谁、什么时候到期」记在内存静态表 {@link #STOLEN} 里 —— 服务端重启后表是空的，
     * 永久加成却还在，没有任何记录能把它收回去（玩家会永久多出护甲 / 生命上限，正是罚抄被报的 bug）。
     * transient modifier 不落盘，重启 / 重登后自然消失，不会留脏数据；到期与目标死亡照旧由
     * {@link #onLivingTick} / {@link #onLivingDeath} 收回去。
     * <p>
     * <b>先 {@code removeModifier(uuid)} 再 add</b>（与 {@link RewriteArmorModifier} 的 {@code apply} 一致）：
     * 同一个 UUID 重复 add 时 {@code AttributeInstance} 会抛 {@code IllegalArgumentException}，
     * 而重算 / 旧存档里残留的同 UUID 永久 modifier 都可能让这个 UUID 已经存在。
     */
    private static void updateBonus(LivingEntity attacker) {
        Map<UUID, Steal> stolen = STOLEN.get(attacker.getUUID());

        double totalArmor = 0;
        double totalHealth = 0;
        if (stolen != null) {
            for (Steal steal : stolen.values()) {
                totalArmor += steal.armor();
                totalHealth += steal.health();
            }
        }

        AttributeInstance armorAttr = attacker.getAttribute(Attributes.ARMOR);
        AttributeInstance healthAttr = attacker.getAttribute(Attributes.MAX_HEALTH);

        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_UUID);
            if (totalArmor > 0) {
                armorAttr.addTransientModifier(new AttributeModifier(ARMOR_UUID, "rewrite_armor", totalArmor, AttributeModifier.Operation.ADDITION));
            }
        }
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_UUID);
            if (totalHealth > 0) {
                healthAttr.addTransientModifier(new AttributeModifier(HEALTH_UUID, "rewrite_health", totalHealth, AttributeModifier.Operation.ADDITION));
            }
        }
    }
}
