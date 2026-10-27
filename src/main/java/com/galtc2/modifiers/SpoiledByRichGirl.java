package com.galtc2.modifiers;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 娇生惯养（spoiled_by_rich_girl）—— 穿戴/手持时给予村庄英雄。
 * <p>
 * <b>为什么改成「短时长 + 定期续期」</b>：{@code EQUIPMENT_CHANGE} 会对手部槽位派发
 * （{@code EquipmentChangeWatcher} 遍历 {@code EquipmentSlot.values()}），所以<b>手持带该词条的弓</b>
 * 也会走 {@code onEquip} / {@code onUnequip}。旧实现给的是 {@code Integer.MAX_VALUE} 时长，
 * 并在 {@code onUnequip} 里<b>无条件</b> {@code removeEffect(HERO_OF_THE_VILLAGE)}：
 * <ul>
 *   <li>把玩家自己（{@code /effect}、村民交易、其它模组）的村庄英雄一起删掉 —— 换下手里的弓就没了；</li>
 *   <li>手部槽位切换频繁（换手/切物品栏/捡起）→ 每次 add/remove 一遍，效果图标一直闪。</li>
 * </ul>
 * 现在只负责「给 + 续」，不再负责「删」：效果 120 tick（6 s），在真正生效的槽位
 * （护甲 {@code isCorrectSlot} / 手持 {@code isSelected}）每 20 tick 续一次，卸下后自然到期，
 * 因此玩家的村庄英雄不会被误删。
 */
public class SpoiledByRichGirl extends Modifier implements EquipmentChangeModifierHook, InventoryTickModifierHook {

    /** 村庄英雄时长（tick）：6 s，靠续期维持；卸下后最多 6 s 自然到期（不再手动删除） */
    private static final int EFFECT_DURATION = 120;

    /** 续期间隔（tick） */
    private static final int REFRESH_INTERVAL = 20;

    public SpoiledByRichGirl() {}

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.EQUIPMENT_CHANGE, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        LivingEntity entity = context.getEntity();
        if (entity instanceof Player player && !player.level().isClientSide()) {
            refresh(player, modifier.getLevel() + 1);
        }
    }

    // 刻意不再实现 onUnequip 的 removeEffect：卸下后让效果自然到期即可。
    // 旧代码在这里无条件删村庄英雄，会把玩家自己的效果一并删掉（见类注释）。

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder,
                                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        // 只在「真正生效的槽位」续期：手持（主手 isSelected / 副手 isCorrectSlot）或护甲的本槽（isCorrectSlot）。
        // 背包里放着不生效，所以不续。
        if (world.isClientSide() || !(holder instanceof Player player) || (!isSelected && !isCorrectSlot)) {
            return;
        }
        if (world.getGameTime() % REFRESH_INTERVAL != 0) {
            return;
        }
        refresh(player, modifier.getLevel() + 1);
    }

    /**
     * 给/续村庄英雄（amplifier 仍是 {@code level + 1}，效果参数保持原样）。
     * <p>
     * 玩家身上已有<b>更长</b>的同类效果（自己 {@code /effect give}、交易或其它模组给的）时
     * <b>完全不碰</b>：原版 {@code MobEffectInstance#update} 在「新等级更高」时会用我们的 120 tick
     * 覆盖掉玩家的长时效果，等于换个方式把人家的效果弄丢，这里先挡掉。
     */
    private static void refresh(Player player, int amplifier) {
        MobEffectInstance current = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        if (current != null && current.getDuration() > EFFECT_DURATION) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE,
                EFFECT_DURATION, amplifier, false, false, true));
    }
}
