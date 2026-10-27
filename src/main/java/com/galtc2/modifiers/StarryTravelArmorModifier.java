package com.galtc2.modifiers;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.UUID;

/**
 * 晴空（护甲侧）—— 银河铁道材料的防御特性：离出生点越远，护甲与坚韧越高。
 * <p>
 * <b>为什么不用 {@code INVENTORY_TICK}</b>：那个钩子只对「玩家背包里的物品」调用，
 * 女仆/怪物穿护甲时根本不 tick → 护甲穿在女仆身上时这条特性完全没反应。
 * 所以这里改成全局 {@link LivingEvent.LivingTickEvent}：任何实体只要穿着带这条特性的护甲，
 * 每秒刷新一次护甲/坚韧加成；玩家那条 {@code INVENTORY_TICK} 路径保留（效果一致，重复刷新无害）。
 */
public class StarryTravelArmorModifier extends BaseModifiers implements InventoryTickModifierHook {

    /** 本词条的 id（用于扫装备时判断护甲上有没有这条特性） */
    public static final slimeknights.tconstruct.library.modifiers.ModifierId MODIFIER_ID =
            new slimeknights.tconstruct.library.modifiers.ModifierId("galtc2", "starry_travel_armor");

    private static final UUID ARMOR_UUID = UUID.fromString("12345678-1234-1234-1234-123456789012");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("23456789-2345-2345-2345-234567890123");

    public StarryTravelArmorModifier() {
        MinecraftForge.EVENT_BUS.addListener(this::onLivingTick);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, net.minecraft.world.level.Level level,
                                LivingEntity entity, int slot, boolean isSelected, boolean isCorrectSlot,
                                ItemStack itemStack) {
        if (level.isClientSide() || !isCorrectSlot) {
            return;
        }
        if (entity.tickCount % 20 != 0) {
            return;
        }
        applyBonus(entity);
    }

    /** 全局 tick：任何实体（含女仆、怪物）穿带这条特性的护甲都生效 */
    private void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity.tickCount % 20 != 0) {
            return;
        }
        if (!wearsArmorTrait(entity)) {
            return;
        }
        applyBonus(entity);
    }

    /** 实体身上是否有带「晴空」的护甲 */
    private static boolean wearsArmorTrait(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !ToolStack.isInitialized(stack)) {
                continue;
            }
            ToolStack tool = ToolStack.from(stack);
            if (tool.isBroken()) {
                continue;
            }
            if (tool.getModifiers().getLevel(StarryTravelArmorModifier.MODIFIER_ID) > 0) {
                return true;
            }
        }
        return false;
    }

    private static void applyBonus(LivingEntity entity) {
        int armorBonus = StarryTravelModifier.getArmorBonus(entity);
        if (armorBonus <= 0) {
            return;
        }
        AttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        AttributeInstance toughnessAttr = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);

        if (armorAttr != null) {
            if (armorAttr.getModifier(ARMOR_UUID) != null) {
                armorAttr.removeModifier(ARMOR_UUID);
            }
            armorAttr.addPermanentModifier(new AttributeModifier(ARMOR_UUID, "starry_travel_armor",
                    armorBonus, AttributeModifier.Operation.ADDITION));
        }
        if (toughnessAttr != null) {
            if (toughnessAttr.getModifier(TOUGHNESS_UUID) != null) {
                toughnessAttr.removeModifier(TOUGHNESS_UUID);
            }
            toughnessAttr.addPermanentModifier(new AttributeModifier(TOUGHNESS_UUID, "starry_travel_toughness",
                    armorBonus, AttributeModifier.Operation.ADDITION));
        }
    }
}
