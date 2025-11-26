package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

//万寿菊
public class Tagetes extends BaseModifiers{
    public Tagetes(){}
    @Override
    public boolean isNoLevels() {
        return false;
    }
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity enemy=context.getLivingTarget();
        LivingEntity player=context.getPlayerAttacker();
        double rate=1-(player.getHealth()/player.getMaxHealth());
        double facor=1+rate+modifier.getLevel()*0.15;
        return (float) (damage*facor);


    }

}
