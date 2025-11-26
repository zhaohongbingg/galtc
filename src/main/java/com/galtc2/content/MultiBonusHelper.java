package com.galtc2.content;


import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import javax.annotation.Nullable;

public record MultiBonusHelper(boolean breakSpeed, boolean meleeDamage, @Nullable INumericToolStat<?> modifyStat) {

    public static final RecordLoadable<MultiBonusHelper> ALL = RecordLoadable.create(
            BooleanLoadable.DEFAULT.defaultField("break_speed", false, MultiBonusHelper::breakSpeed),
            BooleanLoadable.DEFAULT.defaultField("melee_damage", false, MultiBonusHelper::meleeDamage),
            ToolStats.NUMERIC_LOADER.nullableField("modify_stat", MultiBonusHelper::modifyStat),
            MultiBonusHelper::new
    );

    public static MultiBonusHelper addAll(@Nullable INumericToolStat<?> stat) {
        return new MultiBonusHelper(true, true, stat);
    }

    public boolean testStat(FloatToolStat stat) {
        return this.modifyStat == stat;
    }
}