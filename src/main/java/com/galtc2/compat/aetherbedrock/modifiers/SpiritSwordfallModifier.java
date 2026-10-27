package com.galtc2.compat.aetherbedrock.modifiers;

import com.galtc2.Galtc2;
import com.galtc2.compat.aetherbedrock.AetherBedrockEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 灯灵天剑 (spirit_swordfall) —— 天境基石生物重制 联动强化（占用能力槽）。
 * <p>
 * 右键时以<b>玩家为圆心扫描 {@value #RANGE} 格内的敌人</b>（不再依赖准星和固定距离）：
 * <b>每个敌人头上一柄巨剑</b>，最多 {@value #MAX_SWORDS} 柄，按距离由近到远依次落下。
 * 每柄剑都是灯壶之灵本体的 {@code SpiritSkillEntity.SWORD}：落点先出现收缩预警环，
 * 剑从 14 格高空下落，插地爆开双层魂焰冲击环 + 震起地面 + 魂火/碎光，并播放原模组的
 * 落剑 / 插地音效（{@code spirit_sword_fall} / {@code spirit_sword_impact}）。
 * <p>
 * <b>空放</b>：范围内没有敌人时不召剑、也不消耗冷却，可以随便对着空气右键。
 * <p>
 * 伤害由原模组实体结算：默认 12/11 × 面板攻击力，命中 3.4 格范围并挑飞；
 * 数值可以在模组配置 {@code boss_skill_multipliers.spirit.sword} 改。我们自己不结算伤害。
 * <p>
 * 冷却 {@value #COOLDOWN} tick（10 秒），按这把工具各自记（持久数据 + 服务端递减）。
 */
public class SpiritSwordfallModifier extends Modifier
        implements GeneralInteractionModifierHook, InventoryTickModifierHook, TooltipModifierHook {

    /** 冷却剩余 tick（每把工具各算各的） */
    private static final ResourceLocation KEY_COOLDOWN = Galtc2.loc("spirit_swordfall_cooldown");

    /** 冷却（tick）：10 秒 */
    private static final int COOLDOWN = 200;
    /** 搜索半径（格）：以玩家为中心，不依赖准星 */
    private static final double RANGE = 12.0D;
    /** 一次最多召几柄（防止刷怪塔里一口气召上百柄） */
    private static final int MAX_SWORDS = 8;
    /** 第一柄剑的预警 tick（与灯壶之灵本体一致） */
    private static final int BASE_DELAY = 16;
    /** 每多一柄剑，预警往后错开这么多 tick（形成连落） */
    private static final int DELAY_STEP = 3;

    public SpiritSwordfallModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.INVENTORY_TICK, ModifierHooks.TOOLTIP);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player,
                                       InteractionHand hand, InteractionSource source) {
        if (source != InteractionSource.RIGHT_CLICK || tool.isBroken()) {
            return InteractionResult.PASS;
        }
        // 客户端不做判定（冷却与实体生成都在服务端），照 回响震击 / 钟波 的写法
        if (player.level() instanceof ServerLevel level) {
            if (!cast(level, player, tool)) {
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** 对范围内每个敌人召一柄剑；没有敌人 / 冷却中返回 false */
    private static boolean cast(ServerLevel level, Player player, IToolStackView tool) {
        if (tool.getPersistentData().getInt(KEY_COOLDOWN) > 0) {
            return false;
        }
        List<LivingEntity> targets = new ArrayList<>(level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(RANGE), entity -> isEnemy(player, entity)));
        targets.removeIf(entity -> entity.distanceToSqr(player) > RANGE * RANGE);
        if (targets.isEmpty()) {
            // 空放：不召剑也不进冷却，玩家可以随便右键
            return false;
        }
        // 由近到远依次落剑
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        int count = Math.min(targets.size(), MAX_SWORDS);
        boolean spawned = false;
        for (int i = 0; i < count; i++) {
            LivingEntity target = targets.get(i);
            // 剑落在目标身上（脚底高度）：飞行/悬空目标也能吃到落点伤害，不再固定取地面
            Vec3 ground = new Vec3(target.getX(), target.getY(), target.getZ());
            spawned |= AetherBedrockEffects.spiritSwordfall(level, player, ground, BASE_DELAY + i * DELAY_STEP);
        }
        if (!spawned) {
            return false;
        }
        tool.getPersistentData().putInt(KEY_COOLDOWN, COOLDOWN);
        return true;
    }

    /** 敌人判定：排除自己、自己的宠物、队友、死亡与旁观者 */
    private static boolean isEnemy(Player player, LivingEntity entity) {
        if (entity == player || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (entity instanceof TamableAnimal tamable && tamable.isTame() && tamable.getOwner() == player) {
            return false;
        }
        return !entity.isAlliedTo(player);
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
        tooltip.add(Component.translatable("modifier.galtc2.spirit_swordfall.tip")
                .withStyle(ChatFormatting.GRAY));
    }
}
