package com.galtc2.modifiers;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;

import java.util.List;

public class Muramasa extends BaseModifiers {


    public Muramasa() {
    }

    ;

    private static final ResourceLocation NEED_PACIFY = ResourceLocation.fromNamespaceAndPath("modid", "need_pacify");

    /** 射出时把「欠一次和平生物」的状态抄到箭矢上（命中钩子拿不到 tool） */
    private static final ResourceLocation ARROW_PACIFY = ResourceLocation.fromNamespaceAndPath("galtc2", "muramasa_arrow_pacify");
    /** 每支箭只乘一次：baseDamage 是粘性值，穿射时反复乘会滚雪球 */
    private static final ResourceLocation ARROW_DONE = ResourceLocation.fromNamespaceAndPath("galtc2", "muramasa_arrow_done");
    /** 每支箭只扣一次血（命中时扣；穿射穿多只不重复扣） */
    private static final ResourceLocation ARROW_BLED = ResourceLocation.fromNamespaceAndPath("galtc2", "muramasa_arrow_bled");
    /** 命中一次扣多少血 */
    private static final float HIT_HEALTH_COST = 1.0F;

    /**
     * 获取状态
     */
    private static boolean needPacify(ModDataNBT data) {
        return data.getBoolean(NEED_PACIFY);
    }

    /**
     * 设置状态
     */
    private static void setNeedPacify(ModDataNBT data, boolean value) {
        data.putBoolean(NEED_PACIFY, value);
    }


    /**
     * 触发击杀事件
     */
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getPlayerAttacker();
        boolean wasKill = target != null && target.isDeadOrDying();
        if (!wasKill) return;
        if (!wasKill) return;
        if (!(attacker instanceof Player)) return;

        ModDataNBT data = tool.getPersistentData();

        // 是怪物：需要偿还善恶
        if (isHostile(target)) {
            setNeedPacify(data, true);
            attacker.sendSystemMessage(Component.literal("§c你杀死了怪物，需要再杀一个友好生物来平衡善恶。"));
        }
        // 是和平生物：清除状态
        else if (isPeaceful(target)) {
            if (needPacify(data)) {
                setNeedPacify(data, false);
                attacker.sendSystemMessage(Component.literal("§a善恶平衡恢复了。"));
            }
        }
    }

    /**
     * 影响伤害：没还平衡前不给加成
     */
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                float baseDamage, float damage) {

        ModDataNBT data = tool.getPersistentData();
        LivingEntity target = context.getLivingTarget();
        if (isPeaceful(target)) {
            return damage;   // 正常伤害
        }

        // 如果欠一次和平生物，就削弱伤害或关闭特性
        if (needPacify(data)) {
            return damage * 0.1f;
        }

        // 正常逻辑
        return damage * 3;
    }

    /**
     * 是否怪物
     */
    private boolean isHostile(LivingEntity entity) {
        return entity instanceof Monster;
    }

    private boolean isPeaceful(LivingEntity entity) {
        return entity.getType().getCategory() == MobCategory.CREATURE
                || entity.getType().getCategory() == MobCategory.AMBIENT;
    }

    /* ============================ 远程（弓 / 弩）分支 ============================ */

    /**
     * 射出：只把「欠一次和平生物」的状态抄到箭矢上（命中钩子拿不到 tool）。
     * <p>
     * 「用一次扣 1 点血」已按用户口径（2026-10-27）<b>改到命中时扣</b>（见 {@link #onProjectileHitTarget}）：
     * 空放不扣血，命中才付代价，每支箭最多扣一次。
     */
    @Override
    public void onProjectileShoot(IToolStackView bow, ModifierEntry modifier, LivingEntity shooter,
                                  Projectile projectile, AbstractArrow arrow, ModDataNBT modDataNBT, boolean primary) {
        if (arrow == null || shooter == null || shooter.level().isClientSide()) return;
        modDataNBT.putBoolean(ARROW_PACIFY, needPacify(bow.getPersistentData()));
    }

    /**
     * 箭矢命中：沿用近战的「和平生物不变、欠债 ×0.1、正常 ×3」，
     * 但作用在箭矢自身的 baseDamage 上（写回箭矢，穿射的后续目标吃同一份伤害，不会再次相乘）。
     * <p>
     * 同时按用户口径收「用一次扣 1 点血」的代价：命中才扣（空放不扣）、只对玩家、血量 &gt; 1 不致死、
     * 每支箭只扣一次（{@link #ARROW_BLED}，穿射穿多只不会重复扣）。
     */
    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level,
                                      Projectile projectile, AbstractArrow arrow, EntityHitResult hit,
                                      LivingEntity attacker, LivingEntity target) {
        if (arrow == null || attacker == null || target == null) return;
        if (target.level().isClientSide()) return;
        if (target.isDeadOrDying()) return;

        // 用一次扣 1 点血：命中即付，每支箭一次
        if (attacker instanceof Player player && player.getHealth() > HIT_HEALTH_COST
                && !persistentData.contains(ARROW_BLED)) {
            persistentData.putBoolean(ARROW_BLED, true);
            player.hurt(player.damageSources().magic(), HIT_HEALTH_COST);
        }

        if (persistentData.contains(ARROW_DONE)) return;
        // 和平生物不削也不加：和近战一致
        if (isPeaceful(target)) return;

        double base = arrow.getBaseDamage();
        if (base <= 0.0D) return;

        float factor = persistentData.getBoolean(ARROW_PACIFY) ? 0.1F : 3.0F;
        arrow.setBaseDamage(base * factor);
        persistentData.putBoolean(ARROW_DONE, true);
    }


    public void addTooltip(IToolStackView tool,
                           ModifierEntry modifier,
                           @Nullable Player player,
                           List<Component> tooltip,
                           TooltipFlag flag) {
        ModDataNBT data = tool.getPersistentData();
        boolean need = needPacify(data);

// 状态文本
        Component status;
        if (need) {
            status = Component.literal("§c需要偿还善恶：击杀一个和平生物以恢复"); // 红色
        } else {
            status = Component.literal("§a善恶平衡良好"); // 绿色
        }
    }
}








