package com.galtc2.content;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;

/**
 * 四部件护甲的物品实现。
 * <p>
 * 属性、耐久、强化全部来自匠魂那一套（{@link ModifiableArmorItem} + {@link ModifiableArmorMaterial}），
 * 这里只重写一件事：<b>护甲贴图路径</b>。
 * <p>
 * 原因：{@link ModifiableArmorItem} 用的是原版 {@code ArmorItem#getArmorTexture}，而它的
 * {@link slimeknights.tconstruct.library.tools.item.armor.DummyArmorMaterial#getName()} 返回
 * {@code "galtc2:gal"} 这种带冒号的字符串，拼出来的路径是 <code>.../galtc2:gal_layer_1.png</code>，
 * 根本不存在。匠魂自己的护甲靠 {@code MultilayerArmorItem} + {@code ArmorTextureSupplier} 动态取贴图，
 * 那套要配套生成材质图层，测试阶段没必要。
 * <p>
 * 所以这里直接借原版铁甲贴图，先跑通逻辑；等确认四个部件没问题，再换成自绘贴图
 * （把下面的路径改成 {@code galtc2:textures/models/armor/gal_layer_1.png} 并放图即可）。
 */
public class galArmorItem extends ModifiableArmorItem {

    public galArmorItem(ModifiableArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, @Nullable String type) {
        return slot == EquipmentSlot.LEGS
                ? "minecraft:textures/models/armor/iron_layer_2.png"
                : "minecraft:textures/models/armor/iron_layer_1.png";
    }
}
