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
    public azarashi()
    {};

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        // 阈值每次实时读取：static final 会在类加载时就把配置缓存死，改配置不重启不生效
        double threshold = GaltcConfig.COMMON.Azarashi_ATTACK_THRESHOLD.get();
        double totalActtack= builder.getStat(ToolStats.ATTACK_DAMAGE);
        if(totalActtack>threshold){
            ToolStats.ATTACK_DAMAGE.multiply(builder,3.0f);

        }
    }

}
