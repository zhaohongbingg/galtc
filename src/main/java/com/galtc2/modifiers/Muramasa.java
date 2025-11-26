package com.galtc2.modifiers;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
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
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;

import java.util.List;

public class Muramasa extends BaseModifiers {


    public Muramasa() {
    }

    ;

    private static final ResourceLocation NEED_PACIFY = new ResourceLocation("modid", "need_pacify");

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








