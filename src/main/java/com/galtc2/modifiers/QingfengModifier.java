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
 * 清枫 (qingfeng) —— 近战命中或箭矢命中时，在目标处召唤「疫病虫群」。
 * <p>
 * 虫群本体使用 J村 的法术 {@code jerotes:insect_plague}：真正的虫群实体
 * 逻辑由 {@link Swarm} 桥接，在 {@code com.galtc2.compat.jerotes.JerotesCompat}
 * 检测到 J村 时注入（与 {@code EuthanasiaModifier} 的血雨、{@code SayaModifier} 的触手同一套做法）。
 * 未安装 J村 时特性不产生任何效果（材料本身也只在 J村 存在时注册，正常拿不到）。
 * <p>
 * 存在时间随等级成长：1 级 {@link #BASE_SECONDS} 秒，之后每级 +{@link #SECONDS_PER_LEVEL} 秒
 * （10 / 15 / 20 ...），由 {@link #seconds(int)} 统一换算。
 * <p>
 * 攻击力同样随等级成长，但<b>刻意不去改 J村 的法术等级</b>（法术等级恒为原版
 * {@code getBaseSpellLevel()}，避免和 J村 之后要做的"玩家法术"体系互相干扰）：
 * 改为由 J村 联动侧的 {@code JerotesEvents} 按原版 5 tick 的节奏，对虫群范围内的目标
 * 额外调用几次虫群自己的 {@code dealDamageTo}（见 {@link #bites(int)}），
 * 也就是"每次伤害口径不变，只是咬得更勤"。
 * <p>
 * 等级计算：<b>必须手持带清枫的武器才会触发</b>：盔甲上的清枫同样会收到匠魂的近战命中钩子，
 * 所以触发时只认主/副手的等级，空手或手持普通物品一律不触发（只有盔甲带清枫时完全不动）。
 * 触发等级 = 手上清枫等级 + 身上 4 件盔甲清枫等级之和 / {@link #ARMOR_LEVEL_DIVISOR}。
 */
public class QingfengModifier extends BaseModifiers {
    /** 召唤冷却(tick)，避免每次命中都刷出虫群 */
    private static final long COOLDOWN = 40L;
    private static final String KEY_COOLDOWN = "galtc2_qingfeng_cooldown";

    /** 1 级虫群的存在时间（秒） */
    public static final int BASE_SECONDS = 10;
    /** 每高 1 级多出来的存在时间（秒） */
    public static final int SECONDS_PER_LEVEL = 5;

    /** 1 级虫群每轮的撕咬次数（J村 原版为 1 次 / 5 tick） */
    public static final int BASE_BITES = 1;
    /** 每高 1 级追加的撕咬次数（= 攻击力成长），想调强度改这一个数 */
    public static final int BITES_PER_LEVEL = 1;
    /** 每轮撕咬次数上限（防止等级堆叠过高） */
    public static final int BITE_LIMIT = 6;
    /**
     * 写在虫群实体上的「额外撕咬次数」NBT 键。
     * 写入方是 J村 联动侧 {@code JerotesInsectPlagueCaster}，读取方是 {@code JerotesEvents}，
     * 两边都不修改虫群的法术等级。
     */
    public static final String KEY_EXTRA_BITES = "galtc2_qingfeng_bites";

    /** 参与等级加成的盔甲槽位（4 件套） */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    /** 盔甲清枫等级之和要除以这个数再计入总等级（4 件套 ×1 级 → 只算 2 级） */
    private static final int ARMOR_LEVEL_DIVISOR = 2;

    /** 虫群桥接：由 J村 联动模块实现并注入 */
    public interface Swarm {
        /**
         * 在 victim 处召唤疫病虫群。
         *
         * @param caster 召唤者
         * @param victim 虫群中心
         * @param level  清枫有效等级（1 起，已叠加盔甲），存在时间见 {@link #seconds(int)}
         * @return 是否已经由 J村 法术接管
         */
        boolean summon(LivingEntity caster, LivingEntity victim, int level);
    }

    private static volatile Swarm swarm;

    /** 由 J村 联动模块调用注入实现 */
    public static void setSwarm(Swarm impl) {
        swarm = impl;
    }

    public static boolean hasSwarm() {
        return swarm != null;
    }

    /** 等级对应的虫群存在时间（秒）：1 级 10 秒，之后每级 +5 秒 */
    public static int seconds(int level) {
        return BASE_SECONDS + SECONDS_PER_LEVEL * (Math.max(1, level) - 1);
    }

    /**
     * 等级对应的每轮撕咬次数（= 攻击力）：1 级 1 次（原版数值），之后每级 +1 次，
     * 上限 {@link #BITE_LIMIT}。每轮间隔与 J村 原版一致（5 tick），所以 3 级就是 3 倍伤害。
     */
    public static int bites(int level) {
        return Math.min(BITE_LIMIT, BASE_BITES + BITES_PER_LEVEL * (Math.max(1, level) - 1));
    }

    @Override
    public void onAfterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                LivingEntity attacker, LivingEntity target, float damageDealt) {
        if (damageDealt <= 0) return;
        // 盔甲上的清枫也会收到 MELEE_HIT（匠魂对盔甲同样派发近战命中钩子，例如原版盔甲模块 Enderclearance），
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
     * @param weaponLevel 武器侧的清枫等级：近战由 {@link #heldLevel(LivingEntity)} 从主/副手读取，
     *                    远程取自弓弩（箭矢里存的强化数据）。为 0 表示手上不是清枫武器，直接不触发
     */
    private void trigger(LivingEntity attacker, LivingEntity target, int weaponLevel) {
        if (weaponLevel <= 0) return;
        if (!(attacker.level() instanceof ServerLevel server) || target == null) return;
        // 未安装 J村 时没有桥接实现：特性什么都不做（不做任何替代效果）
        Swarm impl = swarm;
        if (impl == null) return;
        long now = server.getGameTime();
        if (now - attacker.getPersistentData().getLong(KEY_COOLDOWN) < COOLDOWN) return;
        attacker.getPersistentData().putLong(KEY_COOLDOWN, now);

        int level = weaponLevel + armorModifierLevel(attacker) / ARMOR_LEVEL_DIVISOR;
        impl.summon(attacker, target, level);
    }

    /** 主手/副手上清枫的等级，取较大者；空手或普通物品返回 0（= 手上不是清枫武器，不触发） */
    private int heldLevel(LivingEntity attacker) {
        return Math.max(heldLevel(attacker, InteractionHand.MAIN_HAND),
                heldLevel(attacker, InteractionHand.OFF_HAND));
    }

    private int heldLevel(LivingEntity attacker, InteractionHand hand) {
        ItemStack stack = attacker.getItemInHand(hand);
        if (stack.isEmpty() || !ToolStack.isInitialized(stack)) return 0;
        return ToolStack.from(stack).getModifiers().getLevel(this.getId());
    }

    /** 四件盔甲上清枫等级之和（盔甲不是匠魂工具、或已损坏时不计入） */
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
