package com.galtc2.modifiers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 腐姬 (euthanasia) —— 近战命中或箭矢命中时，在目标处降下「不洁血雨」。
 * <p>
 * 血雨本体使用 J村 的法术 {@code jerotesvillage:unclean_blood_rain}：真正的降雨
 * 逻辑由 {@link BloodRain} 桥接，在 {@code com.galtc2.compat.jerotes.JerotesCompat}
 * 检测到 J村 时注入（与 {@code SayaModifier} 的触手召唤同一套做法）。
 * 未安装 J村 时特性不产生任何效果（材料本身也只在 J村 存在时注册，正常拿不到）。
 * <p>
 * 等级计算：<b>必须手持带腐姬的武器才会触发</b>：盔甲上的腐姬同样会收到匠魂的近战命中钩子，
 * 所以触发时只认主/副手的等级，空手或手持普通物品一律不触发（只有盔甲带腐姬时完全不动）。
 * 触发等级 = 手上腐姬等级 + 身上 4 件盔甲的腐姬等级之和 / {@link #ARMOR_LEVEL_DIVISOR}。
 * 例如 4 件 1 级腐姬盔甲 + 1 级腐姬武器 = 1 + 4/2 = 3 级，避免盔甲直接把等级顶满。
 */
public class EuthanasiaModifier extends BaseModifiers {
    /** 召唤冷却(tick)，避免每次命中都刷新雨云 */
    private static final long COOLDOWN = 40L;
    private static final String KEY_COOLDOWN = "galtc2_euthanasia_cooldown";

    /** 参与等级加成的盔甲槽位（4 件套） */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    /** 盔甲腐姬等级之和要除以这个数再计入总等级（4 件套 ×1 级 → 只算 2 级） */
    private static final int ARMOR_LEVEL_DIVISOR = 2;

    /** 血雨桥接：由 J村 联动模块实现并注入 */
    public interface BloodRain {
        /**
         * 在 victim 处降下不洁血雨。
         *
         * @param caster 降雨者
         * @param victim 雨云中心
         * @param level  腐姬有效等级（1 起，已叠加盔甲）
         * @return 是否已经由 J村 法术接管
         */
        boolean rain(LivingEntity caster, LivingEntity victim, int level);
    }

    private static volatile BloodRain bloodRain;

    /** 由 J村 联动模块调用注入实现 */
    public static void setBloodRain(BloodRain impl) {
        bloodRain = impl;
    }

    public static boolean hasBloodRain() {
        return bloodRain != null;
    }

    @Override
    public void onAfterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                LivingEntity attacker, LivingEntity target, float damageDealt) {
        if (damageDealt <= 0) return;
        // 盔甲上的腐姬也会收到 MELEE_HIT（匠魂对盔甲同样派发近战命中钩子，例如原版盔甲模块 Enderclearance），
        // 所以不能用 modifier.getLevel() 当"武器等级"，必须回到手上去确认拿的是什么
        trigger(attacker, target, heldLevel(attacker));
    }

    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level,
                                      Projectile projectile, AbstractArrow arrow, EntityHitResult hit,
                                      LivingEntity attacker, LivingEntity target) {
        trigger(attacker, target, level);
    }

    /**
     * @param weaponLevel 武器侧的腐姬等级：近战由 {@link #heldLevel(LivingEntity)} 从主/副手读取，
     *                    远程取自弓弩（箭矢里存的强化数据）。为 0 表示手上不是腐姬武器，直接不触发
     */
    private void trigger(LivingEntity attacker, LivingEntity target, int weaponLevel) {
        if (weaponLevel <= 0) return;
        if (!(attacker.level() instanceof ServerLevel server) || target == null) return;
        // 未安装 J村 时没有桥接实现：特性什么都不做（不做任何替代效果）
        BloodRain impl = bloodRain;
        if (impl == null) return;
        long now = server.getGameTime();
        if (now - attacker.getPersistentData().getLong(KEY_COOLDOWN) < COOLDOWN) return;
        attacker.getPersistentData().putLong(KEY_COOLDOWN, now);

        int level = weaponLevel + armorModifierLevel(attacker) / ARMOR_LEVEL_DIVISOR;
        impl.rain(attacker, target, level);
    }

    /** 主手/副手上腐姬的等级，取较大者；空手或普通物品返回 0（= 手上不是腐姬武器，不触发） */
    private int heldLevel(LivingEntity attacker) {
        return Math.max(heldLevel(attacker, InteractionHand.MAIN_HAND),
                heldLevel(attacker, InteractionHand.OFF_HAND));
    }

    private int heldLevel(LivingEntity attacker, InteractionHand hand) {
        ItemStack stack = attacker.getItemInHand(hand);
        if (stack.isEmpty() || !ToolStack.isInitialized(stack)) return 0;
        return ToolStack.from(stack).getModifiers().getLevel(this.getId());
    }

    /** 四件盔甲上腐姬等级之和（盔甲不是匠魂工具、或已损坏时不计入） */
    private int armorModifierLevel(LivingEntity entity) {
        ModifierId id = this.getId();
        int total = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !ToolStack.isInitialized(stack)) continue;
            ToolStack tool = ToolStack.from(stack);
            if (tool.isBroken()) continue;
            total += tool.getModifiers().getLevel(id);
        }
        return total;
    }
}
