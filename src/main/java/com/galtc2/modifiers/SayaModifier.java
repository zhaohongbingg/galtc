package com.galtc2.modifiers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/**
 * 沙耶 (saya) —— 攻击后召唤触手。
 * <p>
 * 触手来自 J村 的 {@code jerotesvillage:unclean_tentacle}(UncleanTentacleEntity)。
 * 本类不直接引用 J村 类：真正的召唤逻辑由 {@link TentacleSummoner} 桥接，
 * 在 {@code com.galtc2.compat.jerotes.JerotesCompat} 检测到 J村 时注入。
 * 未安装 J村 时 {@link #summoner} 为 null，特性不产生任何效果。
 */
public class SayaModifier extends Modifier implements MeleeHitModifierHook, OnAttackedModifierHook,
        ProjectileHitModifierHook, ProjectileLaunchModifierHook {

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
    /**
     * 射箭时把弓的 {@code ToolStats.PROJECTILE_DAMAGE} 抄到箭矢上：
     * {@code PROJECTILE_HIT} 钩子拿不到 tool（只有 {@code ModifierNBT}），而弓的
     * {@code ATTACK_DAMAGE} 基本是 0，近战那套 {@code tool.getStats().get(ATTACK_DAMAGE) * 0.5} 在弓上会算出 0。
     */
    private static final ResourceLocation KEY_ARROW_DAMAGE = ResourceLocation.fromNamespaceAndPath("galtc2", "saya_arrow_damage");
    /** 召唤伤害相对「投射物伤害」的比例 */
    private static final float SUMMON_DAMAGE_RATIO = 0.5F;

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
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.ON_ATTACKED,
                ModifierHooks.PROJECTILE_HIT, ModifierHooks.PROJECTILE_LAUNCH);
    }

    /**
     * 旧 7 参签名（没有 ammo）：接口里是抽象方法，必须实现；逻辑在同名的 8 参重载里。
     */
    @Override
    @Deprecated
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
                                   Projectile projectile, @Nullable AbstractArrow arrow,
                                   ModDataNBT persistentData, boolean primary) {
    }

    /**
     * 射出：把弓的投射物伤害抄到箭矢上，供命中时的召唤算基数（见 {@link #KEY_ARROW_DAMAGE}）。
     */
    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
                                   net.minecraft.world.item.ItemStack ammo, Projectile projectile,
                                   @Nullable AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
        if (arrow == null) return;
        persistentData.putFloat(KEY_ARROW_DAMAGE, (float) tool.getStats().get(ToolStats.PROJECTILE_DAMAGE));
    }

    /**
     * 箭矢命中：在目标附近召唤触手（与近战同一条 {@link #SUMMON_COOLDOWN} 冷却）。
     * <p>
     * 伤害基数改用弓的 {@code PROJECTILE_DAMAGE}（射箭时已抄到箭矢上），仍 ×{@value #SUMMON_DAMAGE_RATIO}；
     * 拿不到时退回箭矢自己的 baseDamage。未装 J村（summoner == null）静默跳过，和近战一致。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                          Projectile projectile, EntityHitResult hit,
                                          @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        TentacleSummoner impl = summoner;
        if (impl == null || attacker == null || target == null) return false;
        Level level = attacker.level();
        if (level.isClientSide() || target.isDeadOrDying()) return false;

        CompoundTag data = attacker.getPersistentData();
        long now = level.getGameTime();
        if (now - data.getLong(KEY_LAST_SUMMON) < SUMMON_COOLDOWN) return false;
        data.putLong(KEY_LAST_SUMMON, now);

        float base = persistentData.getFloat(KEY_ARROW_DAMAGE);
        if (base <= 0.0F && projectile instanceof AbstractArrow arrow) {
            base = (float) arrow.getBaseDamage();
        }
        impl.summon(level, attacker, target, base * SUMMON_DAMAGE_RATIO);
        return false;
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
