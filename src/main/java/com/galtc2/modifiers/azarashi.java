package com.galtc2.modifiers;


import com.galtc2.GaltcConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.IToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.function.BiConsumer;

public class azarashi extends BaseModifiers{
    private  static final   double Azarashi_ATTACK_THRESHOLD= GaltcConfig.COMMON.Azarashi_ATTACK_THRESHOLD.get();
    public azarashi()
    {};

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
      double totalActtack= builder.getStat(ToolStats.ATTACK_DAMAGE);
        if(totalActtack>Azarashi_ATTACK_THRESHOLD){
            ToolStats.ATTACK_DAMAGE.multiply(builder,3.0f);

        }
    }

}
