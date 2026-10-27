package com.galtc2.compat.littlemaid.modifiers;

import com.galtc2.compat.littlemaid.LittleMaidModifiers;
import com.galtc2.modifiers.BaseModifiers;
import com.galtc2.modifiers.TraitLevels;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 同心 (one_heart) —— 车万女仆 联动护甲词条（材料 traits）。
 * <p>
 * 两条效果，按「谁穿着」分：
 * <ul>
 *   <li><b>玩家穿着</b>：酒狐（玩家的车万女仆 女仆）受到的伤害，
 *       {@value #TRANSFER_RATIO_DISPLAY}% 转由玩家承受，自己只留剩下的部分。
 *       判定放在收到伤害的那一方（女仆）身上，所以穿 4 件也只结算一次；</li>
 *   <li><b>酒狐自己穿着</b>：持续获得<b>抗性提升 II</b> + <b>生命恢复 III</b>
 *       （每 {@value #EFFECT_INTERVAL} tick 续一次，脱下后约 {@value #EFFECT_DURATION} tick 内消失）。</li>
 * </ul>
 * <b>为什么女仆侧要挂在 LivingTickEvent 上</b>：匠魂护甲的 {@code INVENTORY_TICK} 钩子只在
 * 「玩家背包里的物品」上调用，女仆的装备槽不会 tick，所以原来那套护甲钩子对女仆永远不触发。
 * 这里改成全局 LivingTickEvent：女仆身上有这件甲就每 20 tick 补一次药水效果，任何实体都适用。
 */
public class OneHeartModifier extends BaseModifiers {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 转给玩家的比例：80% */
    private static final float TRANSFER_RATIO = 0.8f;

    /** 玩家离酒狐超过这个距离（格）就不转伤：避免隔着半个世界「远程承伤」 */
    private static final double MAX_TRANSFER_DISTANCE = 100.0D;

    /**
     * 重入守卫：{@code owner.hurt(...)} 会再派发一轮 LivingHurtEvent/LivingAttackEvent，
     * 防止嵌套派发把同一次伤害再结算一遍（档案见 docs/analysis/F-001-oneheart.md §7.1）。
     */
    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** 抗性提升 II 的 amplifier（0 = I） */
    private static final int RESISTANCE_AMPLIFIER = 1;

    /** 生命恢复 III 的 amplifier（0 = I） */
    private static final int REGENERATION_AMPLIFIER = 2;

    /** 效果持续时间（tick），比刷新间隔长，避免中间出现空档 */
    private static final int EFFECT_DURATION = 120;

    /** 效果刷新间隔（tick） */
    private static final int EFFECT_INTERVAL = 20;

    /** 描述里的百分比数字 */
    private static final int TRANSFER_RATIO_DISPLAY = 80;

    public OneHeartModifier() {
        MinecraftForge.EVENT_BUS.addListener(this::onLivingTick);
    }

    @Override
    public boolean isSingleLevel() {
        return true;
    }

    /* ------------------------------------------------------------------ 玩家穿：伤害转移 */

    /**
     * 玩家穿同心护甲时，酒狐受到的伤害 80% 转由玩家承受。
     * <p>
     * <b>为什么挂全局 LivingHurtEvent</b>：匠魂护甲的 {@code ON_ATTACKED} / {@code MODIFY_DAMAGE} 钩子
     * 只发给「挨打的那个实体自己穿的甲」（TConstruct {@code ToolEvents.livingAttack} 用
     * {@code new EquipmentContext(event.getEntity())}），酒狐挨打时根本看不到玩家身上的四件甲；
     * {@code LivingHurtEvent} 才是全局能看到「任何实体受伤」的落点。
     * <p>
     * <b>这一版修的是什么</b>（根因见 docs/analysis/F-001-oneheart.md §4/§6/§7）：
     * <ol>
     *   <li><b>结算顺序</b>：旧代码先扣掉酒狐 80%、再调 {@code owner.hurt(...)} 且<b>不看返回值</b>。
     *       只要玩家那侧被原版闸门挡住（创造/旁观 {@code abilities.invulnerable}、自己处于无敌帧、
     *       举盾、{@code amount == 0}），这 80% 就凭空蒸发，而酒狐照样只吃 20% —— 表现为「同心不生效」。
     *       现在改成<b>先给玩家、真的扣到了（返回 true）才</b>减少酒狐那份；付不出就整单不动。</li>
     *   <li><b>伤害源</b>：旧代码的 {@code indirectMagic(maid, owner)} 把<b>玩家自己</b>当成攻击者
     *       （{@code getEntity() == owner}），会踩创造/旁观闸门、按难度缩放（和平难度 = 0 伤害），
     *       还会重复跑攻击者的护甲钩子、把玩家写进自己的 {@code lastHurtByMob}。
     *       现在<b>复用原始伤害源</b>（普通怪物的近战/箭矢照常走护甲、抗性、保护附魔结算，
     *       仇恨也正确转移到攻击者身上）；只有「来源实体就是主人自己」这种自指情况才退回
     *       {@code generic()}（无实体、不吃难度缩放、不在 {@code bypasses_armor} 里，仍然过护甲）。</li>
     *   <li><b>无敌帧</b>：不再写 {@code owner.invulnerableTime = 0} 去硬绕免疫窗口
     *       （那会让玩家在一次攻击里连吃多段，也是转伤时灵时不灵的来源）。
     *       玩家自己在免疫窗口里时 {@code hurt} 返回 false，这一次就按「不转伤、酒狐吃全伤害」处理，
     *       而不是「扣了没人接」。</li>
     * </ol>
     * <b>只结算一次 / 4 件不叠加</b>：判定在「受害者（酒狐）」这一侧，一次伤害只派发一次
     * {@code LivingHurtEvent}；比例固定 0.8，穿 1 件和穿 4 件结果一样；嵌套派发由 {@link #REENTRY} 挡掉。
     */
    @Override
    public void LivingHurtEvent(LivingHurtEvent event) {
        // 递归守卫：owner.hurt() 会再派发事件，这一次不能重复结算
        if (Boolean.TRUE.equals(REENTRY.get())) {
            return;
        }
        if (!(event.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide()) {
            return;
        }
        if (event.isCanceled()) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0f) {
            return;
        }
        if (!JiuHuHelper.isJiuHu(maid)) {
            return;
        }
        if (!(maid.getOwner() instanceof Player owner) || !owner.isAlive()) {
            return;
        }
        // 创造 / 旁观吃不到伤害（Player#hurt 里 abilities.invulnerable 直接返回 false），整单放弃：
        // 让酒狐照常掉自己的血，而不是「先扣 80% 却没人接」
        if (owner.isCreative() || owner.isSpectator()) {
            return;
        }
        if (owner.distanceToSqr(maid) > MAX_TRANSFER_DISTANCE * MAX_TRANSFER_DISTANCE) {
            return;
        }
        if (!wearsOneHeart(owner)) {
            return;
        }
        float transfer = amount * TRANSFER_RATIO;
        if (transfer <= 0.0f) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.getEntity() == owner) {
            // 主人自己打酒狐：这是「自指」来源（TLM 还会把这份伤害 clamp(amount/5, 0, 2)）。
            // 换成无实体的 general：不吃难度缩放、不归属攻击者，但仍然过护甲/附魔结算。
            source = owner.level().damageSources().generic();
        }
        boolean paid = false;
        REENTRY.set(Boolean.TRUE);
        try {
            paid = owner.hurt(source, transfer);
        } catch (RuntimeException exception) {
            // 玩家侧任何意外都不该让酒狐这次受伤丢流程：当作没结算成功，酒狐吃全伤害
            LOGGER.warn("[galtc2] 同心：给玩家结算转移伤害时出错，本次不转移", exception);
        } finally {
            REENTRY.set(Boolean.FALSE);
        }
        LOGGER.debug("[galtc2] one_heart: maid {} transfer {} paid {}", maid.getName().getString(), transfer, paid);

        // 只有玩家真的扣到血，才减少酒狐那份；否则整单放弃（旧代码在这里无条件扣 → 伤害蒸发）
        if (paid) {
            event.setAmount(amount - transfer);
        }
    }

    /**
     * 玩家身上任意一件护甲带同心就算（4 件不叠加，一次伤害只转移一次）。
     * <p>
     * 这里不直接依赖 {@link TraitLevels#armorLevel}：那条路要求护甲是「已初始化的匠魂工具」，
     * 玩家用箱子/潜影盒换出来的匠魂护甲、或被其它模组改写过的护甲会读不到等级而整条词条失效。
     * 所以这里逐件扫护甲槽，直接从护甲的 {@code ModifierNBT} 里读等级，只要有一件 > 0 就算。
     * <p>
     * <b>先用 {@link ToolStack#ensureInitialized(ItemStack)} 补建数据</b>：{@code /give} 出来的、
     * 箱子里换出来的、被别的模组写过的匠魂甲<b>没有 {@code tic_stats}</b>，
     * {@code ToolStack.getModifiers()} 会是空的，整条词条静默失效。TConstruct 自己的合成/掉落钩子
     * 也是用同一个方法补的；它对非匠魂物品是空操作，所以下面先挡掉非 {@link IModifiable} 的甲，
     * 免得 {@code ToolStack.from()} 对原版甲刷 {@code logInvalidToolStack}（默认 WARNING）警告。
     */
    private static boolean wearsOneHeart(Player player) {
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable)) {
                continue;
            }
            // 补建 tic_stats/tic_modifiers：/give、箱子换出来、被别的模组写过的匠魂甲都可能还没初始化，
            // 不补建的话 getModifiers() 是空的，整条词条静默失效（TConstruct 自己的掉落/合成钩子也是这么补的）
            ToolStack.ensureInitialized(stack);
            ToolStack tool = ToolStack.from(stack);
            if (tool.isBroken()) {
                continue;
            }
            if (tool.getModifiers().getLevel(LittleMaidModifiers.ONE_HEART_ID) > 0) {
                return true;
            }
        }
        return false;
    }

    /* ------------------------------------------------------------------ 酒狐穿：抗性 + 回复 */

    private void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide()) {
            return;
        }
        if (!JiuHuHelper.isJiuHu(maid)) {
            return;
        }
        Level level = maid.level();
        if (level.getGameTime() % EFFECT_INTERVAL != 0) {
            return;
        }
        if (TraitLevels.armorLevel(maid, LittleMaidModifiers.ONE_HEART_ID) <= 0) {
            return;
        }
        maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                EFFECT_DURATION, RESISTANCE_AMPLIFIER, true, false, true));
        maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                EFFECT_DURATION, REGENERATION_AMPLIFIER, true, false, true));
    }
}
