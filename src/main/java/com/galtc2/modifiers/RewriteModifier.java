package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

    /** 攻击者身上是否还挂着本次的加成（死亡重生、被其它东西清掉属性时需要补回） */
    private static boolean hasBonus(LivingEntity attacker) {
        AttributeInstance armorAttr = attacker.getAttribute(Attributes.ARMOR);
        AttributeInstance healthAttr = attacker.getAttribute(Attributes.MAX_HEALTH);
        return (armorAttr == null || armorAttr.getModifier(ARMOR_UUID) != null)
                && (healthAttr == null || healthAttr.getModifier(HEALTH_UUID) != null);
    }

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
                armorAttr.addPermanentModifier(new AttributeModifier(ARMOR_UUID, "rewrite_armor", totalArmor, AttributeModifier.Operation.ADDITION));
            }
        }
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_UUID);
            if (totalHealth > 0) {
                healthAttr.addPermanentModifier(new AttributeModifier(HEALTH_UUID, "rewrite_health", totalHealth, AttributeModifier.Operation.ADDITION));
            }
        }
    }
}
