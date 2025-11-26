package com.galtc2.modifiers;

import com.galtc2.Galtc2;
import com.galtc2.GaltcConfig;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.UUID;
import java.util.function.BiConsumer;

public class NightTimeSheep extends BaseModifiers {
    public NightTimeSheep() {
    }
    @Override
    public boolean isNoLevels() {
        return true;
    }

    private static final double ATTACK_THRESHOLD = GaltcConfig.COMMON.ATTACK_THRESHOLD.get();

    private static final UUID NTSuuid = UUID.fromString("7b6a7b6a-7b6a-7b6a-7b6a-7b6a7b6a7b6a");


    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if(damage<ATTACK_THRESHOLD){
            return (float) (damage*(ATTACK_THRESHOLD-damage));
        }
        else{
            return damage;
        }


    }
    }



