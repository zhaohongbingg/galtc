package com.galtc2.compat.jei;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtcBlock;
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
        // Gal 晶簇：工作台用剧情/涩气碎片按形状合成
        registration.addIngredientInfo(
                new ItemStack(galtcBlock.GAL_CORE.get()),
                VanillaTypes.ITEM_STACK,
                Component.literal("工作台合成：剧情碎片放四角、涩气碎片放四边 → 1个Gal晶簇"),
                Component.literal("把 Gal 晶簇贴在 Gal 母岩旁边，母岩会慢慢长出新的晶簇")
        );

        // Gal 母岩：会长晶芽、晶芽长成晶簇；自然生成在 Gal 晶洞里
        registration.addIngredientInfo(
                new ItemStack(galtcBlock.BUDDING_GAL_CORE.get()),
                VanillaTypes.ITEM_STACK,
                Component.literal("Gal 母岩：对相邻空位随机长出晶芽（小→中→大→Gal晶簇）"),
                Component.literal("自然生成在 Gal 晶洞里（地下 y=-58~30，约每24区块一个）")
        );

        // 酒狐锭：酒狐干活时攒下，放进她自己的背包
        registration.addIngredientInfo(
                new ItemStack(galtcItem.jiuhu_ingot.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.galtc2.jiuhu_ingot.1"),
                Component.translatable("jei.galtc2.jiuhu_ingot.2"),
                Component.translatable("jei.galtc2.jiuhu_ingot.3")
        );
    }
}
