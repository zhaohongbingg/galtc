package com.galtc2.modifiers;

import com.galtc2.Register.galtc2Modifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.EnchantmentModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.build.ModifierRequirementsModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.data.ModifierIds;

import javax.annotation.Nullable;
import java.util.Map;

public class SuperLuckyModifier extends Modifier implements EnchantmentModifierHook, ValidateModifierHook {

    private static final int REQUIRED_FORTUNE_LEVEL = 3;

    public SuperLuckyModifier() {}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ENCHANTMENTS, ModifierHooks.VALIDATE);
        // 参考匠魂“不毁”强化：需要“幸运”III 作为前置
        hookBuilder.addModule(ModifierRequirementsModule.builder()
                .requireUpgrade(ModifierIds.luck, 3)
                .modifierKey(galtc2Modifier.super_lucky)
                .build());
    }

    @Override
    public int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Enchantment enchantment, int level) {
        if (enchantment == Enchantments.BLOCK_FORTUNE || enchantment == Enchantments.MOB_LOOTING) {
            return level + modifier.getLevel() * 20;
        }
        return level;
    }

    @Override
    public void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Enchantment, Integer> map) {
        EnchantmentModifierHook.addEnchantment(map, Enchantments.BLOCK_FORTUNE, modifier.getLevel() * 10);
        EnchantmentModifierHook.addEnchantment(map, Enchantments.MOB_LOOTING, modifier.getLevel() * 10);
    }

    @Nullable
    @Override
    public Component validate(IToolStackView tool, ModifierEntry modifier) {
        return null;
    }
}
