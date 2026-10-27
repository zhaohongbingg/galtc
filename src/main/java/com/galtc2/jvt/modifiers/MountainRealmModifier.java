package com.galtc2.jvt.modifiers;

import com.galtc2.compat.jerotes.JerotesAnesthesia;
import com.jerotes.jerotes.init.JerotesMobEffects;
import com.jerotes.jerotes.util.AttackFind;
import com.jerotes.jerotes.util.EntityAndItemFind;
import com.jerotes.jerotes.util.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.List;

/**
 * 镇山(mountain_realm) 材料特性。
 * 复刻 J村 镇山套，但不再区分部位：身上任意一件带该特性的护甲即可获得全部效果
 * - 使直视的目标获得震慑(ABACK)；
 * - 近战攻击把小型生物击飞；
 * - 疾跑时给附近生物虚弱；
 * - 清除自身缓慢与虚弱。
 * <p>
 * <b>箭矢分支（F-029，用户口径「给予麻醉效果（j村里的）」）</b>：
 * 远程命中 {@value #RANGED_ANESTHESIA_CHANCE} 概率给目标施加 J村 的麻醉
 * ({@code jerotes:anesthetized})，时长 {@value #ANESTHESIA_TICKS_PER_LEVEL} tick × 等级，
 * 效果等级 {@code max(0, level - 1)}（1 级 = 麻醉 I）。
 * 计时与概率口径照 {@link ChainBindModifier}（J村 麻醉投团同档）。
 * <p>
 * 刻意<b>不复用</b> {@code afterMeleeHit}：那条分支开头就有 {@code countPieces(attacker) <= 0} 的
 * 四件护甲门槛（只穿弓时恒为 0 → 直接 return），远程必须走自己的 {@code PROJECTILE_HIT}。
 * 近战行为（震慑/击飞/虚弱/清 debuff）保持原样，不动。
 * <p>
 * 麻醉本身是幂等 debuff，同一支箭对同一目标只掷一次骰子（箭矢 {@code persistentData} 标记）。
 */
public class MountainRealmModifier extends Modifier implements InventoryTickModifierHook, MeleeHitModifierHook, ProjectileHitModifierHook {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    /** 远程麻醉：命中时的触发概率 */
    private static final float RANGED_ANESTHESIA_CHANCE = 0.4f;
    /** 远程麻醉：每级持续时间（tick），1 级 = 10 秒 */
    private static final int ANESTHESIA_TICKS_PER_LEVEL = 200;
    /** 箭矢分支的去重标记（写在箭矢的 persistentData 上） */
    private static final ResourceLocation RANGED_ROLLED =
            ResourceLocation.fromNamespaceAndPath("galtc2", "mountain_realm_ranged_rolled");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK, ModifierHooks.MELEE_HIT, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide || !isCorrectSlot || !holder.isAlive()) {
            return;
        }
        // 不限制部位：任意一件带该特性的护甲即可
        if (countPieces(holder) <= 0) {
            return;
        }
        holder.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        holder.removeEffect(MobEffects.WEAKNESS);
        if (holder instanceof Player player) {
            Entity target = Main.getTargetedEntity(player, 32.0);
            if (target instanceof LivingEntity living && !AttackFind.FindCanNotAttack(player, living)) {
                living.addEffect(new MobEffectInstance(JerotesMobEffects.ABACK.get(), 5, 0), player);
            }
        }
        if (holder.isSprinting() && holder.tickCount % 5 == 0) {
            List<LivingEntity> nearby = holder.level().getEntitiesOfClass(LivingEntity.class, holder.getBoundingBox().inflate(6.0));
            for (LivingEntity living : nearby) {
                if (living == holder || !living.isAlive() || AttackFind.FindCanNotAttack(holder, living)) {
                    continue;
                }
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1), holder);
            }
        }
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target == null || attacker == null || !target.isAlive() || countPieces(attacker) <= 0) {
            return;
        }
        if (EntityAndItemFind.isNoSpecialKnockback(target.getType())) {
            return;
        }
        if (!Main.mobSizeSmall(target) && !Main.mobSizeMedium(target) && !Main.mobSizeLarge(target)) {
            return;
        }
        double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double power = Math.max(0.0, 1.0 - resistance) + 0.3;
        target.setOnGround(false);
        target.setDeltaMovement(target.getDeltaMovement().add(
                -((attacker.getX() - target.getX()) * power / 3.0) * 0.8666667,
                -((attacker.getY() - target.getY()) * 1.5 * power) * 0.8666667,
                -((attacker.getZ() - target.getZ()) * power / 3.0) * 0.8666667));
    }

    /**
     * 箭矢命中：40% 概率给目标挂 J村 麻醉（200 tick × 等级）。
     * <p>
     * 这里<b>不检查</b>四件护甲门槛——远程特性挂在弓的 {@code perStat.ranged} 上，
     * 命中就说明手上的弓带该特性；只有穿护甲空手/持普通弓时这个 hook 根本不会派发。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         LivingEntity attacker, LivingEntity target) {
        // 服务端权威 + 空值/存活守卫
        if (projectile.level().isClientSide() || target == null || attacker == null || !target.isAlive()) {
            return false;
        }
        int modLevel = modifier.getLevel();
        if (modLevel <= 0) {
            return false;
        }
        // 每支箭对同一目标只掷一次骰子（穿射不重复吃概率）
        ResourceLocation key = rangedKey(target);
        if (persistentData.getBoolean(key)) {
            return false;
        }
        persistentData.putBoolean(key, true);

        if (target.level().getRandom().nextFloat() > RANGED_ANESTHESIA_CHANCE) {
            return false;
        }
        // J村 麻醉 helper 内部处理"J村 未装 → 返回 false"，这里不额外判 ModList
        JerotesAnesthesia.apply(target.level(), target, ANESTHESIA_TICKS_PER_LEVEL * modLevel,
                Math.max(0, modLevel - 1));
        return false;
    }

    /** 去重键 = 固定标记 + 命中目标 id（穿射同一支箭打不同目标时各自掷一次） */
    private static ResourceLocation rangedKey(LivingEntity target) {
        return ResourceLocation.fromNamespaceAndPath("galtc2", RANGED_ROLLED.getPath() + "_" + target.getId());
    }

    private boolean has(LivingEntity holder, EquipmentSlot slot) {
        return ModifierUtil.getModifierLevel(holder.getItemBySlot(slot), this.getId()) > 0;
    }

    private int countPieces(LivingEntity holder) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (has(holder, slot)) {
                count++;
            }
        }
        return count;
    }
}
