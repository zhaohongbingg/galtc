package com.galtc2.compat.littlemaid.tinkers;

import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidAttackStrafingTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidRangedWalkToTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;

import java.util.List;
import java.util.Map;

/**
 * 「匠魂弓术」女仆任务 —— 让车万女仆识别并使用匠魂的弓/长弓/弩（{@code ModifiableLauncherItem}）。
 * <p>
 * <b>为什么原版不行</b>：女仆的 {@code TaskBowAttack.isWeapon/hasBow} 用的是
 * {@code itemStack.getItem() instanceof BowItem}，而匠魂的弓是
 * {@code ModifiableLauncherItem extends ProjectileWeaponItem}，不继承 {@code BowItem}。
 * <p>
 * <b>做法</b>：注册一个 {@link IRangedAttackTask}（通过官方 {@code ILittleMaid#addMaidTask}），
 * 复用女仆自家的 {@link MaidAttackStrafingTask} / {@link MaidRangedWalkToTarget}，
 * 射击时直接调用匠魂弓自己的 {@code releaseUsing}（按满弓 20 tick），拉弓速度/弹射物伤害/
 * 弹药类型/强化全部走匠魂自己的逻辑。
 * <p>
 * 另外这个任务会：
 * <ul>
 *   <li>索敌范围按弓的标准给到 24 格（默认 {@code searchRadius} 用的是女仆自己的活动半径）；</li>
 *   <li>如果女仆手里没拿弓但背包里有（匠魂发射器 + 它能用的弹药），自动把弓换到主手；</li>
 *   <li>带节流的 INFO 诊断日志，方便排查「为什么不攻击」。</li>
 * </ul>
 */
public final class TinkersBowMaidTask implements IRangedAttackTask {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 任务 UID */
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("galtc2", "tinkers_bow_attack");

    /** 任务图标：用匠魂的弓臂（材料件），避免拿一把没有材料的空弓当图标 */
    private static final ResourceLocation ICON_ITEM = ResourceLocation.fromNamespaceAndPath("tconstruct", "bow_limb");

    /** 索敌范围（格） */
    private static final double SEARCH_RANGE = 24.0D;
    /** 两次射击的最小间隔（tick） */
    private static final int SHOT_COOLDOWN = 24;
    /** 满弓所需 tick */
    private static final int FULL_DRAW_TICKS = 20;

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(BuiltInRegistries.ITEM.get(ICON_ITEM));
    }

    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return SoundEvents.ARROW_SHOOT;
    }

    @Override
    public net.minecraft.network.chat.MutableComponent getName() {
        return Component.translatable("task.galtc2.tinkers_bow_attack");
    }

    @Override
    public List<String> getDescription(EntityMaid maid) {
        return List.of(Component.translatable("task.galtc2.tinkers_bow_attack.desc").getString());
    }

    @Override
    public String getMaidActionSummary() {
        return "Attack hostile mobs with a Tinkers bow, longbow or crossbow from range. "
                + "Requires a Tinkers launcher in the maid's main hand and compatible ammo (arrows) in her inventory.";
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        // 注意：必须返回“可变 List”。女仆的 refreshBrain 会往这个列表里追加额外 AI，
        // 之前返回 ImmutableList 会抛 UnsupportedOperationException，导致切换任务直接失败。
        List<Pair<Integer, BehaviorControl<? super EntityMaid>>> tasks = Lists.newArrayList();
        tasks.add(Pair.of(0, new TinkersEquipBehavior()));
        tasks.add(Pair.of(1, StartAttacking.create(
                (EntityMaid m) -> hasLauncher(m) && hasAmmo(m),
                IRangedAttackTask::findFirstValidAttackTarget)));
        tasks.add(Pair.of(2, StopAttackingIfTargetInvalid.create()));
        tasks.add(Pair.of(3, MaidRangedWalkToTarget.create(1.0F)));
        tasks.add(Pair.of(4, new MaidAttackStrafingTask()));
        tasks.add(Pair.of(5, new TinkersShootBehavior()));
        return tasks;
    }

    @Override
    public float searchRadius(EntityMaid maid) {
        return (float) SEARCH_RANGE;
    }

    @Override
    public AABB searchDimension(EntityMaid maid) {
        return maid.getBoundingBox().inflate(SEARCH_RANGE, 8.0D, SEARCH_RANGE);
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return isLauncher(stack) && hasAmmoFor(maid, stack);
    }

    @Override
    public boolean canSee(EntityMaid maid, LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }
        return maid.distanceToSqr(target) <= SEARCH_RANGE * SEARCH_RANGE && maid.hasLineOfSight(target);
    }

    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float charge) {
        shoot(maid, target);
    }

    // ---------------------------------------------------------------- 工具方法

    /** 主手是不是匠魂的发射类武器（弓 / 长弓 / 弩） */
    public static boolean hasLauncher(EntityMaid maid) {
        return isLauncher(maid.getMainHandItem());
    }

    public static boolean isLauncher(ItemStack stack) {
        return stack.getItem() instanceof ModifiableLauncherItem;
    }

    /** 女仆背包里有没有这把匠魂弓能用的弹药 */
    public static boolean hasAmmo(EntityMaid maid) {
        return hasAmmoFor(maid, maid.getMainHandItem());
    }

    public static boolean hasAmmoFor(EntityMaid maid, ItemStack launcherStack) {
        if (!(launcherStack.getItem() instanceof ProjectileWeaponItem launcher)) {
            return false;
        }
        var inventory = maid.getAvailableInv(false);
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && launcher.getAllSupportedProjectiles().test(stack)) {
                return true;
            }
        }
        return false;
    }

    /** 用匠魂弓自己的一套逻辑射一箭（满弓） */
    public static void shoot(EntityMaid maid, LivingEntity target) {
        ItemStack stack = maid.getMainHandItem();
        if (!(stack.getItem() instanceof ModifiableLauncherItem launcher)) {
            return;
        }
        if (!hasAmmoFor(maid, stack)) {
            return;
        }
        maid.getLookControl().setLookAt(target, 30.0F, 30.0F);
        maid.setSwingingArms(true);
        int useDuration = launcher.getUseDuration(stack);
        int timeLeft = Math.max(0, useDuration - FULL_DRAW_TICKS);
        // 满弓释放：击发、弹药判定、耐久消耗、强化效果全部交给匠魂
        launcher.releaseUsing(stack, maid.level(), maid, timeLeft);
    }

    /**
     * 自动把背包里的匠魂弓换到主手（弹药还在背包里，匠魂自己的弹药钩子会去找）。
     */
    private static boolean equipLauncherFromInventory(EntityMaid maid) {
        if (hasLauncher(maid)) {
            return true;
        }
        var inventory = maid.getAvailableInv(false);
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!isLauncher(stack) || !hasAmmoFor(maid, stack)) {
                continue;
            }
            // 先把主手旧物品放回背包，再把弓换到主手
            ItemStack old = maid.getMainHandItem().copy();
            inventory.setStackInSlot(i, old);
            maid.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            LOGGER.info("[galtc2] 匠魂弓术：已自动把 {} 从背包换到女仆主手", stack.getHoverName().getString());
            return true;
        }
        return false;
    }

    /** 换弓行为：主手没弓但背包里有（且弹药齐全）时自动装备 */
    private static final class TinkersEquipBehavior extends Behavior<EntityMaid> {

        private TinkersEquipBehavior() {
            super(Map.of());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
            return !hasLauncher(maid);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
            return !hasLauncher(maid);
        }

        @Override
        protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
            equipLauncherFromInventory(maid);
        }
    }

    /**
     * 射击行为：每 {@value #SHOT_COOLDOWN} tick 朝当前攻击目标射一箭。
     * 不自己做拉弓动画（女仆手臂由 setSwingingArms 表现），避免和目标记忆/使用物品状态打架。
     */
    private static final class TinkersShootBehavior extends Behavior<EntityMaid> {

        private long nextShotTime;
        private int shots;
        private int logTicks;

        private TinkersShootBehavior() {
            super(Map.of());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
            return hasLauncher(maid) && hasAmmo(maid) && getTarget(maid) != null;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
            LivingEntity target = getTarget(maid);
            return hasLauncher(maid) && hasAmmo(maid) && target != null && target.isAlive();
        }

        @Override
        protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
            this.nextShotTime = gameTime;
            this.shots = 0;
        }

        @Override
        protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
            maid.setSwingingArms(false);
        }

        @Override
        protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
            LivingEntity target = getTarget(maid);
            if (target == null) {
                return;
            }
            // 诊断：每 60 tick 打一行，方便排查「为什么不射」
            if (++this.logTicks >= 60) {
                this.logTicks = 0;
                LOGGER.info("[galtc2] 匠魂弓术诊断：任务={}，主手={}，有弹药={}，目标距离={}，视线={}，已射 {} 箭",
                        maid.getTask().getUid(),
                        maid.getMainHandItem().getHoverName().getString(),
                        hasAmmo(maid),
                        String.format("%.1f", Math.sqrt(maid.distanceToSqr(target))),
                        maid.hasLineOfSight(target),
                        this.shots);
            }
            if (gameTime < this.nextShotTime) {
                return;
            }
            if (!maid.hasLineOfSight(target) || maid.distanceToSqr(target) > SEARCH_RANGE * SEARCH_RANGE) {
                return;
            }
            this.nextShotTime = gameTime + SHOT_COOLDOWN;
            this.shots++;
            shoot(maid, target);
        }

        private static LivingEntity getTarget(EntityMaid maid) {
            return maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        }
    }
}
