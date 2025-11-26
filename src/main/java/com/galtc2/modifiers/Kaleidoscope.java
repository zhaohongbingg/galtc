package com.galtc2.modifiers;

import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class Kaleidoscope extends BaseModifiers{

    public Kaleidoscope(){
    }
    @Override
    public boolean isNoLevels() {
        return false;
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
     int level = modifier.getLevel();
     float multiplier = (float) Math.pow(3, level);
        ToolStats.ATTACK_DAMAGE.multiplyAll(builder, multiplier);
        ToolStats.ATTACK_SPEED.multiplyAll(builder, multiplier);
        ToolStats.PROJECTILE_DAMAGE.multiplyAll(builder, multiplier);
        ToolStats.ARMOR.multiplyAll(builder, multiplier);
        ToolStats.ARMOR_TOUGHNESS.multiplyAll(builder, multiplier);
        ToolStats.KNOCKBACK_RESISTANCE.multiplyAll(builder, multiplier);

    }
}
