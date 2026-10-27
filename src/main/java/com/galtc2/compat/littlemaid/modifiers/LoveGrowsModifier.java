package com.galtc2.compat.littlemaid.modifiers;

import com.galtc2.Galtc2;
import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 日久生情 (love_grows) —— 车万女仆 联动工具词条（材料 traits）。
 * <p>
 * 效果：<b>按酒狐（玩家的车万女仆 女仆）的好感度，为带该词条的工具额外补 1/2/2/3/3/4 点基础面板</b>。
 * <ul>
 *   <li>「基础面板」= 耐久 / 攻击伤害 / 挖掘速度 各 +N 点（材料面板上的那三项）。</li>
 *   <li><b>可叠加</b>：词条等级 = 用该材料做成的部件数，N 会乘以等级
 *       （两件部件都是这个材料 → 双倍加成）。</li>
 *   <li>好感度 6 档按每 {@value com.galtc2.compat.littlemaid.modifiers.JiuHuHelper#POINTS_PER_BAND}
 *       点切分，满好感给最高的 4 点；身边没有酒狐时按最低档 1 点算。</li>
 * </ul>
 * <b>为什么要「先写进持久数据、再重算属性」</b>：匠魂的工具属性是烘焙在工具 NBT 里的，
 * {@code addToolStats} 拿不到「持有者」，也就读不到好感度。所以这里沿用
 * {@code SkilledModifier} 的做法 —— inventory tick 里（拿得到持有者）把当前档位写进工具自己的
 * persistentData，档位一变就调 {@link ToolStack#rebuildStats()} 重算一次，
 * {@code addToolStats} 只负责把持久数据里那个数字加到面板上。
 */
public class LoveGrowsModifier extends BaseModifiers {

    /** 当前生效的「每级加成点数」存在工具持久数据里的键 */
    private static final ResourceLocation APPLIED_BONUS = Galtc2.loc("love_grows_bonus");

    public LoveGrowsModifier() {
    }

    /* ------------------------------------------------------------------ 属性 */

    @Override
    public void modifierAddToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        int perLevel = context.getPersistentData().getInt(APPLIED_BONUS);
        if (perLevel <= 0) {
            return;
        }
        int total = perLevel * modifier.getLevel();
        ToolStats.DURABILITY.add(builder, total);
        ToolStats.ATTACK_DAMAGE.add(builder, total);
        ToolStats.MINING_SPEED.add(builder, total);
    }

    /**
     * 每 20 tick 对着「持有者身边的酒狐」重算一次档位；
     * 档位（好感度）变了才写持久数据 + 重算属性，避免每 tick 都在做重活。
     */
    @Override
    public void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder,
                                        int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide() || tool.isBroken()) {
            return;
        }
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        // 在场酒狐与魂符里的酒狐取好感度最高的那只；被收进魂符时不会掉回 +1
        int bonus = JiuHuHelper.affectionBonusFor(holder);
        if (bonus == tool.getPersistentData().getInt(APPLIED_BONUS)) {
            return;
        }
        // 先写数据再重算：rebuildStats 会重新跑 addToolStats，读到的必须是新值
        tool.getPersistentData().putInt(APPLIED_BONUS, bonus);
        if (tool instanceof ToolStack stack) {
            stack.rebuildStats();
        }
    }

    /* ------------------------------------------------------------------ 显示 */

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player,
                           List<Component> tooltip, TooltipKey key, TooltipFlag flag) {
        int perLevel = tool.getPersistentData().getInt(APPLIED_BONUS);
        if (perLevel <= 0) {
            return;
        }
        tooltip.add(Component.translatable("modifier.galtc2.love_grows.bonus",
                        perLevel * modifier.getLevel())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
