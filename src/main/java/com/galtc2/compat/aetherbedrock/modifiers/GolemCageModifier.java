package com.galtc2.compat.aetherbedrock.modifiers;

import com.galtc2.Galtc2;
import com.galtc2.compat.aetherbedrock.AetherBedrockEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
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
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * 晶笼 (golem_cage) —— 天境基石生物重制 联动强化（护甲侧，占用升级槽）。
 * <p>
 * 受到伤害时在攻击者脚下升起水晶石傀儡的「晶笼」：破土的一下会顶起地面、迸出碎晶并造成
 * 原模组的晶笼破土伤害，随后晶柱停留约 1.7 秒，停留期间站在柱身里的生物持续受伤
 * （表现与伤害全部由原模组的 {@code GolemSkillEntity} + 碎晶粒子 + 音效演出）。
 * 我们自己只额外补一条「被笼住」的控制：缓慢 II + 挖掘疲劳。
 * <p>
 * 冷却 {@value #COOLDOWN} tick（5 秒），记在这件护甲自己的持久数据里；
 * 四件护甲都带这条时只让「第一件」结算（{@link #isFirstPiece}），避免一次受击反四次。
 * <b>必须装在匠魂护甲上</b>：{@code ON_ATTACKED} 钩子只在护甲/盾牌侧触发。
 */
public class GolemCageModifier extends Modifier
        implements OnAttackedModifierHook, InventoryTickModifierHook, TooltipModifierHook {

    /** 冷却剩余 tick（每件护甲各算各的） */
    private static final ResourceLocation KEY_COOLDOWN = Galtc2.loc("golem_cage_cooldown");

    /** 冷却（tick）：5 秒 */
    private static final int COOLDOWN = 100;
    /** 缓慢 / 挖掘疲劳时长（tick）：3 秒 */
    private static final int EFFECT_TICKS = 60;

    /** 参与「第一件」判定的护甲槽位 */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public GolemCageModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED, ModifierHooks.INVENTORY_TICK, ModifierHooks.TOOLTIP);
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                           EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity holder = context.getEntity();
        if (holder == null || amount <= 0.0F) {
            return;
        }
        Level level = holder.level();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == holder || !attacker.isAlive()) {
            return;
        }
        // 四件都带时只在「第一件」上结算
        if (!isFirstPiece(context, slotType)) {
            return;
        }
        if (tool.getPersistentData().getInt(KEY_COOLDOWN) > 0) {
            return;
        }
        tool.getPersistentData().putInt(KEY_COOLDOWN, COOLDOWN);

        // 在攻击者脚下升起原模组的晶笼：破土伤害 + 停留期持续伤害 + 碎晶特效 + 音效
        AetherBedrockEffects.golemCage(serverLevel, holder, attacker);
        // 额外补一条控制：被晶柱罩住的生物行动变慢
        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_TICKS, 1), holder);
        attacker.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, EFFECT_TICKS, 0), holder);
    }

    /** 四件护甲里，按 头→胸→腿→靴 的顺序，只有第一件带这条强化的护甲负责结算 */
    private boolean isFirstPiece(EquipmentContext context, EquipmentSlot slotType) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(this) > 0) {
                return slot == slotType;
            }
        }
        return false;
    }

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
        tooltip.add(Component.translatable("modifier.galtc2.golem_cage.tip")
                .withStyle(ChatFormatting.GRAY));
    }
}
