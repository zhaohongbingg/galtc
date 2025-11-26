package com.galtc2.content;

import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

public record LevelingFormula(float factor, Type operation, boolean single) {

    public static final EnumLoadable<Type> TYPE_LOADER = new EnumLoadable<>(Type.class);
    public static final RecordLoadable<LevelingFormula> LOADER = RecordLoadable.create(
            FloatLoadable.ANY.requiredField("factor", LevelingFormula::factor),
            TYPE_LOADER.requiredField("operation", LevelingFormula::operation),
            BooleanLoadable.DEFAULT.defaultField("single", false, LevelingFormula::single),
            LevelingFormula::new
    );

    public static LevelingFormula add(float factor) {
        return new LevelingFormula(factor, LevelingFormula.Type.addition, false);
    }

    public static LevelingFormula mulBase(float factor) {
        return new LevelingFormula(factor, LevelingFormula.Type.multiplier_base, false);
    }

    public static LevelingFormula mulTotal(float factor) {
        return new LevelingFormula(factor, LevelingFormula.Type.multiplier_total, false);
    }

    public float apply(float origin, ModifierEntry entry) {
        return this.apply(origin, entry.getLevel());
    }

    public float apply(float origin, ModifierEntry entry, float variable) {
        return this.apply(origin, entry.getLevel(), variable);
    }

    public float apply(float origin, int lv) {
        int finalLv = this.single ? 1 : lv;
        return switch (this.operation) {
            case addition -> origin + this.factor * (float) finalLv;
            case multiplier_base -> origin * (1.0F + this.factor * (float) finalLv);
            case multiplier_total -> origin * this.factor * (float) finalLv;
        };
    }

    public float apply(float origin, int lv, float variable) {
        int finalLv = this.single ? 1 : lv;
        float finalFactor = this.factor * variable;
        return switch (this.operation) {
            case addition -> origin + finalFactor * (float) finalLv;
            case multiplier_base -> origin * (1.0F + finalFactor * (float) finalLv);
            case multiplier_total -> origin * finalFactor * (float) finalLv;
        };
    }

    public enum Type {
        addition,
        multiplier_base,
        multiplier_total;
    }
}