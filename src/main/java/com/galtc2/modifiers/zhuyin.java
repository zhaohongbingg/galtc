package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class zhuyin  extends  BaseModifiers{
    @Override
    public boolean isNoLevels() {
        return false;
    }
    public  zhuyin(){}

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity target = context.getLivingTarget();
        if (target != null) {
            return baseDamage + modifier.getLevel() * 10;
        }
        return baseDamage;
    }

}
