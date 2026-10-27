package com.galtc2.compat.aetherbedrock.modifiers;

import com.galtc2.Galtc2;
import com.galtc2.compat.aetherbedrock.AetherBedrockEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * 娜迦震地 (naga_quake) —— 天境基石生物重制 联动强化（占用升级槽）。
 * <p>
 * 命中时把天境娜迦的「巨石」原样召到目标头上：落点先出现秘符预警环，巨石砸落后触发
 * 秘符冲击环 + 震起地面 + 碎石 + 爆炸（全部由原模组的 {@code NagaSkillEntity} 演出），
 * 伤害也走原模组的「巨石撞击」倍率（默认 10/6 × 面板攻击力，可在模组配置里改）。
 * <p>
 * 为什么不再自己算伤害：原技能实体落地时已经结算一次范围伤害 + 击飞，再叠一份就是我们自己
 * 在制造双倍伤害；交给原模组既统一数值口径，也让配置项（{@code boss_skill_multipliers}）继续生效。
 * <p>
 * 触发概率 {@value #BASE_CHANCE}（每级 +{@value #CHANCE_PER_LEVEL}，上限 {@value #MAX_CHANCE}）；
 * 冷却 {@value #COOLDOWN} tick（3 秒），记在这件工具自己的持久数据里（不用原版物品冷却，
 * 否则同类型工具会互相牵连）。
 */
public class NagaQuakeModifier extends Modifier
        implements MeleeHitModifierHook, InventoryTickModifierHook, TooltipModifierHook {

    /** 冷却剩余 tick（每把工具各算各的） */
    private static final ResourceLocation KEY_COOLDOWN = Galtc2.loc("naga_quake_cooldown");

    /** 冷却（tick）：3 秒 */
    private static final int COOLDOWN = 60;
    /** 基础触发概率 */
    private static final float BASE_CHANCE = 0.25F;
    /** 每级增加的概率 */
    private static final float CHANCE_PER_LEVEL = 0.10F;
    /** 概率上限 */
    private static final float MAX_CHANCE = 0.75F;

    public NagaQuakeModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.INVENTORY_TICK, ModifierHooks.TOOLTIP);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker == null || target == null || damageDealt <= 0.0F) {
            return;
        }
        Level level = attacker.level();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (tool.getPersistentData().getInt(KEY_COOLDOWN) > 0) {
            return;
        }
        int lvl = Math.max(1, modifier.getLevel());
        float chance = Math.min(MAX_CHANCE, BASE_CHANCE + CHANCE_PER_LEVEL * (lvl - 1));
        if (serverLevel.random.nextFloat() >= chance) {
            return;
        }
        tool.getPersistentData().putInt(KEY_COOLDOWN, COOLDOWN);
        // 直接把原模组的巨石召到目标头上：预警环、落石、震地、碎石、伤害全由它负责
        AetherBedrockEffects.nagaQuake(serverLevel, attacker, target.position());
    }

    /** 冷却递减：每 tick 减 1，只在服务端写 */
    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder,
                                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (level.isClientSide()) {
            return;
        }
        int cooldown = tool.getPersistentData().getInt(KEY_COOLDOWN);
        if (cooldown > 0) {
            tool.getPersistentData().putInt(KEY_COOLDOWN, cooldown - 1);
        }
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player,
                           List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        int lvl = Math.max(1, modifier.getLevel());
        int chance = Math.round(Math.min(MAX_CHANCE, BASE_CHANCE + CHANCE_PER_LEVEL * (lvl - 1)) * 100.0F);
        tooltip.add(Component.translatable("modifier.galtc2.naga_quake.tip", chance)
                .withStyle(ChatFormatting.GRAY));
    }
}
