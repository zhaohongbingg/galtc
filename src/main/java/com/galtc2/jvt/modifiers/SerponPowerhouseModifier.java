package com.galtc2.jvt.modifiers;

import com.galtc2.compat.jerotes.FireAffinityModifier;

import com.galtc2.jvt.Jvt;

import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 蛇龙强者强化(serpon_powerhouse)
 * 仿照"贵族强化"：前置部件含蛇龙合金，在匠魂站用蛇龙强者升级模板+蛇龙合金锭打上。
 * 每件降低 15% 火系承伤、提升 20% 火系输出。
 */
public class SerponPowerhouseModifier extends FireAffinityModifier implements ValidateModifierHook {

    private static final MaterialId SERPON = new MaterialId(Jvt.MODID, "serpon_alloy");

    @Override
    public float fireTakenReduction() {
        return 0.15f;
    }

    @Override
    public float fireDealtBonus() {
        return 0.20f;
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.VALIDATE);
    }

    @Override
    public Component validate(IToolStackView tool, ModifierEntry modifier) {
        for (MaterialVariant variant : tool.getMaterials().getList()) {
            if (variant.matches(SERPON)) {
                return null;
            }
        }
        return Component.translatable("modifier.galtc2.serpon_powerhouse.requirements");
    }
}
