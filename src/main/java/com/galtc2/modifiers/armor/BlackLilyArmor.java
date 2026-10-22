package com.galtc2.modifiers.armor;

import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;

/**
 * 黑莉莉（护甲侧）：按已损失生命提升护甲，并对「高生命 / 高护甲」的攻击者减伤。
 * <p>
 * 与工具侧两条特性正好镜像：
 * <ul>
 *   <li>{@code galtc2:tagetes}：按攻击者已损失生命提升伤害 → 这里按自己已损失生命提升护甲；</li>
 *   <li>{@code galtc2:lavare}：对高生命 / 高护甲目标额外伤害 → 这里对高生命 / 高护甲的攻击者减伤。</li>
 * </ul>
 */
public class BlackLilyArmor extends BaseModifiers {

    private static final UUID ARMOR_UUID = UUID.fromString("4f9d3a57-ae86-4c14-9025-7b0c6e3d8f04");
    private static final String ARMOR_NAME = "galtc2:blacklily_armor";
    /** 生命见底时每级补的护甲 */
    private static final double MAX_ARMOR_PER_LEVEL = 15.0D;
    /** 每满足一个条件（高生命 / 高护甲）的减伤 */
    private static final float REDUCTION_PER_CONDITION = 0.15F;
    private static final float MAX_REDUCTION = 0.30F;

    public BlackLilyArmor() {
    }

    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide() || !isCorrectSlot || !(holder instanceof Player player)) {
            return;
        }
        if (!ArmorAttributeHelper.isFirstPiece(player, this, itemStack, itemSlot)) {
            return;
        }
        float maxHealth = player.getMaxHealth();
        float lostRatio = maxHealth <= 0.0F ? 0.0F : 1.0F - (player.getHealth() / maxHealth);
        lostRatio = Math.max(0.0F, Math.min(1.0F, lostRatio));
        double bonus = lostRatio * MAX_ARMOR_PER_LEVEL * modifier.getLevel();
        ArmorAttributeHelper.setArmor(player, ARMOR_UUID, ARMOR_NAME, bonus);
    }

    @Override
    public float onModifyTakeDamage(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity holder = context.getEntity();
        if (holder == null || amount <= 0.0F) {
            return amount;
        }
        if (!isFirstPiece(context, slotType)) {
            return amount;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return amount;
        }
        float reduction = 0.0F;
        if (attacker.getMaxHealth() > holder.getMaxHealth()) {
            reduction += REDUCTION_PER_CONDITION;
        }
        if (attacker.getAttributeValue(Attributes.ARMOR) > holder.getAttributeValue(Attributes.ARMOR)) {
            reduction += REDUCTION_PER_CONDITION;
        }
        reduction = Math.min(reduction, MAX_REDUCTION);
        return amount * (1.0F - reduction);
    }

    @Override
    public void modifierOnUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (context.getEntity() instanceof Player player) {
            ArmorAttributeHelper.clearArmor(player, ARMOR_UUID);
        }
    }

    /** 只在第一件带该特性的护甲上结算减伤，避免四件叠加 */
    private boolean isFirstPiece(EquipmentContext context, EquipmentSlot slotType) {
        for (EquipmentSlot slot : ArmorAttributeHelper.ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(this) > 0) {
                return slot == slotType;
            }
        }
        return false;
    }
}
