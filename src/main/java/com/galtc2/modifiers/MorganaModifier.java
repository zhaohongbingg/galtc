package com.galtc2.modifiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.List;
import java.util.Set;

/**
 * 莫尔加那 (morgana) —— 诅咒系材料。
 * <p>
 * <b>攻击侧</b>：近战 / 箭矢命中时给目标挂一个<b>随机 debuff</b>，持续 {@link #DURATION} tick（10 秒）。
 * 随机池是运行时的 {@code ForgeRegistries.MOB_EFFECTS}，也就是 <b>Minecraft 本体 + 所有已加载模组</b>
 * 注册的全部效果里筛出来的"有害且非瞬时"效果（见 {@link #pool()}），因此装了什么模组就能 roll 到什么模组的 debuff。
 * 同一目标重复命中同一种 debuff 会叠一层强度（上限 {@link #MAX_AMPLIFIER}），命中不同种则各自并存。
 * <p>
 * <b>防御侧</b>：穿戴者受击时（{@link OnAttackedModifierHook}）给攻击者挂一个随机 debuff，等级取该件盔甲上的等级。
 * <p>
 * 触发等级沿用 {@link TraitLevels}：攻击侧必须手持带莫尔加那的武器，等级 = 手上等级 + 4 件盔甲之和 / 2；
 * 远程侧取弓弩自身等级（箭矢里存的强化数据）再叠加盔甲加成。
 */
public class MorganaModifier extends Modifier implements MeleeHitModifierHook, ProjectileHitModifierHook, OnAttackedModifierHook {

    /** debuff 持续时间（tick）= 10 秒 */
    public static final int DURATION = 200;
    /** 单种 debuff 能叠到的最高强度（0 起，4 = V 级），与醉梦（sakura）一致 */
    public static final int MAX_AMPLIFIER = 4;
    /** 攻击侧冷却（tick），避免一刀一层叠太快 */
    private static final long ATTACK_COOLDOWN = 40L;
    /** 防御侧冷却（tick），被连打时每秒最多反咒一次 */
    private static final long DEFENSE_COOLDOWN = 20L;
    private static final String KEY_ATTACK = "galtc2_morgana_attack";
    private static final String KEY_DEFENSE = "galtc2_morgana_defense";

    /**
     * 想屏蔽某些效果就写在这里（效果 id，如 {@code "minecraft:bad_omen"}）。
     * 默认空；某个模组的 debuff 表现异常时可以单独拉黑。
     */
    private static final Set<String> BLACKLIST = Set.of();

    /** 随机池：注册表在加载完成后不再变化，缓存一次即可 */
    private static volatile List<MobEffect> pool;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.PROJECTILE_HIT, ModifierHooks.ON_ATTACKED);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker == null || target == null || attacker.level().isClientSide() || damageDealt <= 0) return;
        int level = TraitLevels.weaponLevel(attacker, this.getId());
        if (level <= 0 || !ready(attacker, KEY_ATTACK, ATTACK_COOLDOWN)) return;
        curse(target, level);
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null || attacker.level().isClientSide() || !(projectile instanceof AbstractArrow)) {
            return false;
        }
        if (!ready(attacker, KEY_ATTACK, ATTACK_COOLDOWN)) return false;
        // 远程的"武器"是弓弩本身：等级 = 弓弩上的等级（箭矢里存的强化数据）+ 盔甲加成
        int level = modifier.getLevel() + TraitLevels.armorLevel(attacker, this.getId()) / TraitLevels.ARMOR_LEVEL_DIVISOR;
        curse(target, level);
        return false;
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType,
                           DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity wearer = context.getEntity();
        if (wearer == null || wearer.level().isClientSide() || amount <= 0) return;
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == wearer) return;
        if (!ready(wearer, KEY_DEFENSE, DEFENSE_COOLDOWN)) return;
        // 防御侧等级取这件盔甲上的等级
        curse(attacker, Math.max(1, modifier.getLevel()));
    }

    /**
     * 给目标挂一个随机 debuff：10 秒，重复命中同一种会叠一层强度（上限 {@link #MAX_AMPLIFIER}）。
     *
     * @param level 有效等级，1 级 = 原版强度（amplifier 0），每高 1 级强度 +1
     */
    public static void curse(LivingEntity target, int level) {
        List<MobEffect> available = pool();
        if (available.isEmpty()) return;
        MobEffect effect = available.get(target.getRandom().nextInt(available.size()));
        MobEffectInstance old = target.getEffect(effect);
        int amplifier = (old == null ? 0 : old.getAmplifier() + 1) + (Math.max(1, level) - 1);
        target.addEffect(new MobEffectInstance(effect, DURATION, Math.min(MAX_AMPLIFIER, amplifier), false, true, true));
    }

    /**
     * 可用的 debuff 池：Minecraft + 所有模组注册的"有害且非瞬时"效果。
     * <ul>
     *   <li>只取 {@link MobEffectCategory#HARMFUL}，所以增益类（力量、再生…）和中性类（不祥之兆…）不会出现</li>
     *   <li>排除瞬时效果（瞬间伤害 / 瞬间治疗…），它们的持续时间没有意义</li>
     *   <li>再排除 {@link #BLACKLIST} 里手动屏蔽的</li>
     * </ul>
     */
    private static List<MobEffect> pool() {
        List<MobEffect> cached = pool;
        if (cached == null) {
            cached = ForgeRegistries.MOB_EFFECTS.getValues().stream()
                    .filter(effect -> effect.getCategory() == MobEffectCategory.HARMFUL)
                    .filter(effect -> !effect.isInstantenous())
                    .filter(effect -> {
                        ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                        return key == null || !BLACKLIST.contains(key.toString());
                    })
                    .toList();
            pool = cached;
        }
        return cached;
    }

    /** 冷却检查：冷却存在实体自己身上，通过则记录当前时间 */
    private static boolean ready(LivingEntity entity, String key, long cooldown) {
        long now = entity.level().getGameTime();
        if (now - entity.getPersistentData().getLong(key) < cooldown) return false;
        entity.getPersistentData().putLong(key, now);
        return true;
    }
}
