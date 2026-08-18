package com.galtc2.compat.jei;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtcItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class Galtc2JeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(Galtc2.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // gal_core 说明：用于合成 Gal唱片（简写，不列具体材料）
        registration.addIngredientInfo(
                new ItemStack(galtcItem.gal_core.get()),
                VanillaTypes.ITEM_STACK,
                Component.literal("用于合成 Gal唱片(Gal Record)"),
                Component.literal("合成配方: 8个Gal核心 + 1本书 → 1个Gal唱片")
        );

        // gal_record 说明：作为匠魂强化材料使用（未来开放强化功能）
        registration.addIngredientInfo(
                new ItemStack(galtcItem.gal_record.get()),
                VanillaTypes.ITEM_STACK,
                Component.literal("匠魂强化材料，用于强化工具（功能开发中）")
        );
    }
}
