package com.galtc2.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class lavare extends BaseModifiers{
    public lavare(){};
    @Override
    public boolean isNoLevels() {
        return false;
    }
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity enemy=context.getLivingTarget();
        LivingEntity player=context.getPlayerAttacker();

        double armor = enemy.getAttributeValue(Attributes.ARMOR);
        boolean hasArmor = armor > 0;
      float facor1=1.0f;
        float factor2=1.0f;

        if(enemy.getMaxHealth()> player.getMaxHealth()) {
           facor1= (float) (1+ Math.min((enemy.getMaxHealth()- player.getMaxHealth()) / 100.0, 1.0));
        }
        if(hasArmor){
            factor2= (float) (1+Math.min(Math.sqrt(armor) * 0.05, 0.5));
        }
        float factor3= (float) (modifier.getLevel()*0.2+1);
        return damage*facor1*factor2*factor3;
    }

}
