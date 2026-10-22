package com.galtc2.modifiers.armor;

import com.galtc2.modifiers.BaseModifiers;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;

/**
 * SPRB（护甲侧）：每级 +2 防御槽位。
 * <p>
 * 工具侧 {@code galtc2:sprb} 给的是升级 / 能力槽，这条只给护甲的防御槽。
 */
public class SprbArmor extends BaseModifiers implements VolatileDataModifierHook {

    private static final int DEFENSE_SLOTS_PER_LEVEL = 2;

    public SprbArmor() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.VOLATILE_DATA);
    }

    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT volatileData) {
        volatileData.addSlots(SlotType.DEFENSE, DEFENSE_SLOTS_PER_LEVEL * modifier.getLevel());
    }
}
