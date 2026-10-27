package com.galtc2.modifiers;

import com.galtc2.Galtc2;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockBreakModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;

/**
 * 你怎么这么熟练 (skilled) —— <b>用得越多，工具越强</b>。
 * <p>
 * 获取：工匠站上用 1 个万花镜 + 2 个雪块 + 2 个蓝冰安装，
 * 消耗 1 个能力槽（配方见 {@code data/galtc2/recipes/tool/modifiers/ability/skilled.json}）。
 * <p>
 * 效果：<b>工具每被使用 100 次，永久 +1% 全属性</b>（攻击伤害 / 攻击速度 / 挖掘速度 /
 * 耐久 / 弹射伤害 / 拉弓速度 / 初速度 / 护甲 / 护甲韧性 / 格挡值），无上限。
 * <ul>
 *   <li>「使用」= 挖掉一个方块、打中一次怪、射出一次主手弹药，各记 1 次。</li>
 *   <li>次数存在<b>这把工具自己的 persistentData</b> 里（和克拉罗罗同一套写法），
 *       拆掉强化时一起清空。</li>
 *   <li>匠魂的工具属性是<b>烘焙</b>在 NBT 里的（{@code addToolStats} 只在重算属性时跑），
 *       所以普通地改 persistentData 不会让属性变动。这里的做法是：
 *       使用次数跨过 100 的整数倍时打一个「待刷新」标记，下一次 inventory tick
 *       再调 {@link ToolStack#rebuildStats()} 重算 —— 既避免在挖方块/命中的回调里
 *       做重活，也保证每个 tick 最多重算一次。</li>
 * </ul>
 */
public class SkilledModifier extends Modifier implements
        ToolStatsModifierHook,
        TooltipModifierHook,
        ModifierRemovalHook,
        BlockBreakModifierHook,
        MeleeHitModifierHook,
        ProjectileLaunchModifierHook,
        InventoryTickModifierHook {

    /** 使用次数在工具持久化数据里的键 */
    private static final ResourceLocation USES = Galtc2.loc("skilled_uses");
    /** 已经烘焙进属性里的档数，用来判断需不需要重算 */
    private static final ResourceLocation APPLIED = Galtc2.loc("skilled_applied");

    /** 每多少次使用 +1% 属性 */
    public static final int USES_PER_PERCENT = 100;
    /** 每一档的百分比（0.01 = +1%） */
    public static final float PERCENT_PER_STEP = 0.01f;

    /**
     * 参与加成的工具属性。
     * 用 {@link INumericToolStat#percent} 叠加（多次叠加是相加而不是相乘，正好对应「每 100 次 +1%」）。
     */
    private static final INumericToolStat<?>[] STATS = {
            ToolStats.DURABILITY,
            ToolStats.ATTACK_DAMAGE,
            ToolStats.ATTACK_SPEED,
            ToolStats.MINING_SPEED,
            ToolStats.PROJECTILE_DAMAGE,
            ToolStats.DRAW_SPEED,
            ToolStats.VELOCITY,
            ToolStats.ARMOR,
            ToolStats.ARMOR_TOUGHNESS,
            ToolStats.BLOCK_AMOUNT
    };

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this,
                ModifierHooks.TOOL_STATS,
                ModifierHooks.TOOLTIP,
                ModifierHooks.REMOVE,
                ModifierHooks.BLOCK_BREAK,
                ModifierHooks.MELEE_HIT,
                ModifierHooks.PROJECTILE_LAUNCH,
                ModifierHooks.INVENTORY_TICK);
    }

    /* ------------------------------------------------------------------ 计数 */

    private static void addUse(IToolStackView tool) {
        ModDataNBT data = tool.getPersistentData();
        data.putInt(USES, data.getInt(USES) + 1);
    }

    /** 挖掉一个方块算一次 */
    @Override
    public void afterBlockBreak(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
        addUse(tool);
    }

    /** 打中一次算一次（钩子本身就是服务端触发，这里再兜一次底） */
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (context.getAttacker().level().isClientSide()) {
            return;
        }
        addUse(tool);
    }

    /** 射出一次算一次，只算主手那一发 */
    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
                                   Projectile projectile, @Nullable AbstractArrow arrow, ModDataNBT persistentData,
                                   boolean primary) {
        if (!primary || shooter.level().isClientSide()) {
            return;
        }
        addUse(tool);
    }

    /* ------------------------------------------------------------------ 属性 */

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        int steps = context.getPersistentData().getInt(USES) / USES_PER_PERCENT;
        if (steps <= 0) {
            return;
        }
        float percent = steps * PERCENT_PER_STEP;
        for (INumericToolStat<?> stat : STATS) {
            stat.percent(builder, percent);
        }
    }

    /**
     * 用 tick 做「延迟重算」：次数刚跨过档位时 persistentData 已经变了，
     * 但本 tick 的 ToolStack 属性还是旧的，下一次 tick 统一重建一次。
     */
    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder,
                                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide()) {
            return;
        }
        int steps = tool.getPersistentData().getInt(USES) / USES_PER_PERCENT;
        if (steps <= tool.getPersistentData().getInt(APPLIED)) {
            return;
        }
        if (!(tool instanceof ToolStack stack)) {
            return;
        }
        // 先重算再记档：万一 rebuildStats 因为数据还没加载完而空转，下个 tick 会再试一次
        stack.rebuildStats();
        tool.getPersistentData().putInt(APPLIED, steps);
    }

    /* ------------------------------------------------------------------ 显示 / 清理 */

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player,
                           List<Component> tooltip, TooltipKey key, TooltipFlag flag) {
        int uses = tool.getPersistentData().getInt(USES);
        int steps = uses / USES_PER_PERCENT;
        tooltip.add(Component.translatable("modifier.galtc2.skilled.current",
                        String.format("%.0f", steps * PERCENT_PER_STEP * 100f), uses)
                .withStyle(ChatFormatting.GOLD));
    }

    @Nullable
    @Override
    public Component onRemoved(IToolStackView tool, Modifier modifier) {
        // 强化被拆掉时把次数和档位一起清掉，免得下次重装直接接着吃旧进度
        tool.getPersistentData().remove(USES);
        tool.getPersistentData().remove(APPLIED);
        return null;
    }
}
