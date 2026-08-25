package com.galtc2.modifiers;

import com.galtc2.Galtc2;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 月社妃（Tsukiyashiro Kisaki）防具特性
 *
 * 用作防具时：
 * - 每穿着一个游戏日（24000 tick）获得 4% 全属性减伤，上限 80%（多件叠加累积，总减伤仍封顶 80%）
 * - 代价：绑定诅咒（原版诅咒附魔 + 代码兜底，无法手动卸下）
 * - 受到火焰伤害翻倍，且无视本减伤
 * - 着火后无法熄灭（持续重新点燃）
 * - 因火焰伤害死亡时：减伤清零并强制卸下防具
 */
public class KisakiModifier extends BaseModifiers {

    /** 修饰器 id */
    public static final ResourceLocation ID = Galtc2.loc("kisaki");
    /** NBT 键：累积穿着 tick */
    public static final ResourceLocation KEY = Galtc2.loc("kisaki_data");

    /** 游戏内一天 = 24000 tick */
    private static final long DAY_TICKS = 24000L;
    /** 每穿一天 +4% 减伤 */
    private static final float REDUCTION_PER_DAY = 0.04f;
    /** 减伤上限 80% */
    private static final float MAX_REDUCTION = 0.80f;
    /** 火焰伤害倍率 */
    private static final float FIRE_MULTIPLIER = 2.0f;
    /** 无法熄灭：重新点燃的火焰时长（tick），取 20 的倍数保证火焰伤害正常结算 */
    private static final int REIGNITE_TICKS = 24000;

    /** 无法熄灭：正在燃烧且穿着月社妃防具的玩家 */
    private static final Set<UUID> BURNING = ConcurrentHashMap.newKeySet();
    /** 火焰致死强制卸下：本次死亡流程中放行绑定 */
    private static final Set<UUID> FORCE_UNEQUIP = ConcurrentHashMap.newKeySet();

    public KisakiModifier() {
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener((LivingEquipmentChangeEvent event) -> onEquipmentChange(event));
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerTick);
    }

    /* ===== 减伤 / 火焰翻倍 ===== */

    @Override
    public float onModifyTakeDamage(IToolStackView tool, ModifierEntry modifier,
                                    EquipmentContext context, EquipmentSlot slotType,
                                    DamageSource source, float amount, boolean isDirectDamage) {
        if (amount <= 0) return amount;
        if (slotType.getType() != EquipmentSlot.Type.ARMOR) return amount;
        // 只有第一件月社妃防具结算，其余透传，避免多件翻倍/重复减伤
        if (!isFirstKisakiSlot(context, slotType)) return amount;

        if (source.is(DamageTypeTags.IS_FIRE)) {
            // 火焰伤害：翻倍且无视本减伤
            return amount * FIRE_MULTIPLIER;
        }

        // 全属性减伤（多件叠加，总上限 80%）
        float reduction = getTotalReduction(context);
        if (reduction > 0) {
            amount = amount * (1f - reduction);
            if (amount <= 0) return 0f;
        }
        return amount;
    }

    /** 判断该槽位是否为第一件穿着中的月社妃防具（按装备槽顺序） */
    private boolean isFirstKisakiSlot(EquipmentContext context, EquipmentSlot slotType) {
        for (EquipmentSlot slot : ModifiableArmorMaterial.ARMOR_SLOTS) {
            IToolStackView tool = context.getToolInSlot(slot);
            if (tool != null && tool.getModifierLevel(this) > 0) {
                return slot == slotType;
            }
        }
        return false;
    }

    /** 全套装总减伤：所有穿着中的月社妃防具减伤相加，封顶 80% */
    private float getTotalReduction(EquipmentContext context) {
        float total = 0f;
        for (EquipmentSlot slot : ModifiableArmorMaterial.ARMOR_SLOTS) {
            IToolStackView tool = context.getToolInSlot(slot);
            if (tool != null && tool.getModifierLevel(this) > 0) {
                total += getPieceReduction(tool);
            }
        }
        return Math.min(MAX_REDUCTION, total);
    }

    /** 单件减伤：min(80%, 穿着天数 * 4%) */
    private static float getPieceReduction(IToolStackView tool) {
        CompoundTag tag = tool.getPersistentData().getCompound(KEY);
        long wornTicks = tag.getLong("wornTicks");
        float reduction = (float) wornTicks / DAY_TICKS * REDUCTION_PER_DAY;
        return Math.min(MAX_REDUCTION, reduction);
    }

    /* ===== 穿着计时 / 绑定诅咒附魔 / 点火标记 ===== */

    @Override
    public void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier,
                                        Level level, LivingEntity holder, int itemSlot,
                                        boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide()) return;
        if (!isCorrectSlot) return;
        if (holder.getEquipmentSlotForItem(itemStack).getType() != EquipmentSlot.Type.ARMOR) return;
        if (tool.isBroken()) return;

        // 绑定诅咒附魔兜底：保证存在（原版 GUI 会因 mayPickup 拒绝卸下）
        if (!EnchantmentHelper.hasBindingCurse(itemStack)) {
            itemStack.enchant(Enchantments.BINDING_CURSE, 1);
        }

        // 穿着计时：每 tick +1
        CompoundTag tag = tool.getPersistentData().getCompound(KEY);
        tag.putLong("wornTicks", tag.getLong("wornTicks") + 1L);
        tool.getPersistentData().put(KEY, tag);

        // 着火 → 标记为"无法熄灭"
        if (holder.getRemainingFireTicks() > 0) {
            BURNING.add(holder.getUUID());
        }
    }

    /* ===== 绑定诅咒兜底：卸下强制穿回 ===== */

    private void onEquipmentChange(LivingEquipmentChangeEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!(entity instanceof Player player)) return;
        EquipmentSlot slot = event.getSlot();
        if (slot.getType() != EquipmentSlot.Type.ARMOR) return;
        ItemStack from = event.getFrom();
        if (from.isEmpty()) return;
        if (!event.getTo().isEmpty()) return; // 非卸下（更换/穿上）放行
        if (FORCE_UNEQUIP.contains(player.getUUID())) return;
        if (!(from.getItem() instanceof ModifiableArmorItem)) return;
        if (ToolStack.from(from).getModifierLevel(this) <= 0) return;

        // 从背包找回同一件防具并穿回（针对 shift 快捷移动等绕过 mayPickup 的路径）
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, from)) {
                ItemStack restored = player.getInventory().removeItem(i, 1);
                player.setItemSlot(slot, restored);
                break;
            }
        }
    }

    /* ===== 火焰死亡：减伤清零 + 强制卸下 ===== */

    private void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.getSource().is(DamageTypeTags.IS_FIRE)) return;

        boolean any = false;
        for (EquipmentSlot slot : ModifiableArmorMaterial.ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ModifiableArmorItem)) continue;
            IToolStackView tool = ToolStack.from(stack);
            if (tool.getModifierLevel(this) <= 0) continue;
            any = true;
            // 减伤清零
            CompoundTag tag = tool.getPersistentData().getCompound(KEY);
            tag.putLong("wornTicks", 0L);
            tool.getPersistentData().put(KEY, tag);
        }

        if (any) {
            UUID id = player.getUUID();
            FORCE_UNEQUIP.add(id);   // 本次死亡流程放行绑定
            BURNING.remove(id);      // 停止"无法熄灭"标记
            // 强制卸下：防具放回背包（无 keepInventory 时随死亡掉落）
            for (EquipmentSlot slot : ModifiableArmorMaterial.ARMOR_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty()) continue;
                if (stack.getItem() instanceof ModifiableArmorItem
                        && ToolStack.from(stack).getModifierLevel(this) > 0) {
                    player.setItemSlot(slot, ItemStack.EMPTY);
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
        }
    }

    /* ===== 无法熄灭 + 标记清理 ===== */

    private void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;
        UUID id = player.getUUID();

        if (BURNING.contains(id)) {
            // 已死亡或不再穿着月社妃防具 → 停止燃烧
            if (player.isDeadOrDying() || !wearsKisakiArmor(player)) {
                BURNING.remove(id);
                return;
            }
            // 无法熄灭：火焰熄灭（自然/水）后立即重新点燃
            if (player.getRemainingFireTicks() <= 0) {
                player.setRemainingFireTicks(REIGNITE_TICKS);
            }
        }

        // 复活后清理强制卸下标记
        if (FORCE_UNEQUIP.contains(id) && !player.isDeadOrDying()) {
            FORCE_UNEQUIP.remove(id);
        }
    }

    private boolean wearsKisakiArmor(Player player) {
        for (EquipmentSlot slot : ModifiableArmorMaterial.ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ModifiableArmorItem
                    && ToolStack.from(stack).getModifierLevel(this) > 0) {
                return true;
            }
        }
        return false;
    }

    /* ===== 提示信息 ===== */

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifierEntry,
                           @Nullable Player player, List<Component> tooltip,
                           TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        // 仅防具显示描述，武器上不显示
        if (!(tool.getItem() instanceof ModifiableArmorItem)) return;
        CompoundTag tag = tool.getPersistentData().getCompound(KEY);
        long wornTicks = tag.getLong("wornTicks");
        float reduction = getPieceReduction(tool);
        double days = wornTicks / (double) DAY_TICKS;
        tooltip.add(Component.literal(
                        String.format("已穿着 %.1f 天 · 减伤 %.0f%%（上限80%%）", days, reduction * 100))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("火焰伤害翻倍且无视减伤 · 着火后无法熄灭")
                .withStyle(ChatFormatting.RED));
    }
}
