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
        // 攻击者可能是任意生物（生物拿工具打人也算），不能用 getPlayerAttacker()，否则玩家以外会拿到 null
        LivingEntity attacker = context.getAttacker();
        if (attacker == null || attacker.getMaxHealth() <= 0) {
            return damage;
        }
        double rate = 1 - (attacker.getHealth() / attacker.getMaxHealth());
        double factor = 1 + rate + modifier.getLevel() * 0.15;
        return (float) (damage * factor);
    }

}
