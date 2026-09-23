package com.galtc2.modifiers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/**
 * 沙耶 (saya) —— 攻击后召唤触手。
 * <p>
 * 触手来自 J村 的 {@code jerotesvillage:unclean_tentacle}(UncleanTentacleEntity)。
 * 本类不直接引用 J村 类：真正的召唤逻辑由 {@link TentacleSummoner} 桥接，
 * 在 {@code com.galtc2.compat.jerotes.JerotesCompat} 检测到 J村 时注入。
 * 未安装 J村 时 {@link #summoner} 为 null，特性不产生任何效果。
 */
public class SayaModifier extends Modifier implements MeleeHitModifierHook, OnAttackedModifierHook {

    /** 召唤桥接：由 J村 联动模块实现并注入 */
    public interface TentacleSummoner {
        void summon(Level level, LivingEntity attacker, LivingEntity target, float damage);
        void summonDefense(Level level, LivingEntity defender, LivingEntity attacker, float damage);
    }

    private static volatile TentacleSummoner summoner;

    /** 召唤冷却(tick)，避免每次命中都刷触手 */
    private static final long SUMMON_COOLDOWN = 40L;
    private static final String KEY_LAST_SUMMON = "galtc2_saya_last_tentacle";
    private static final String KEY_LAST_DEFENSE = "galtc2_saya_last_defense";

    /** 由 J村 联动模块调用注入实现 */
    public static void setSummoner(TentacleSummoner impl) {
        summoner = impl;
    }

    public static boolean hasSummoner() {
        return summoner != null;
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        TentacleSummoner impl = summoner;
        if (impl == null) return;

        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker == null || target == null) return;
        Level level = attacker.level();
        if (level.isClientSide()) return;

        CompoundTag data = attacker.getPersistentData();
        long now = level.getGameTime();
        if (now - data.getLong(KEY_LAST_SUMMON) < SUMMON_COOLDOWN) return;
        data.putLong(KEY_LAST_SUMMON, now);

        float toolDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
        impl.summon(level, attacker, target, toolDamage * 0.5f);
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType,
                           DamageSource source, float amount, boolean isDirectDamage) {
        TentacleSummoner impl = summoner;
        LivingEntity defender = context.getEntity();
        if (impl == null || defender.level().isClientSide() || amount <= 0) return;
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;
        CompoundTag data = defender.getPersistentData();
        long now = defender.level().getGameTime();
        if (now - data.getLong(KEY_LAST_DEFENSE) < SUMMON_COOLDOWN) return;
        data.putLong(KEY_LAST_DEFENSE, now);
        impl.summonDefense(defender.level(), defender, attacker, 20.0f);
    }
}
