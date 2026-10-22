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
        LivingEntity target = context.getLivingTarget();
        // 攻击者可能是任意生物（生物拿工具打人也算），不能用 getPlayerAttacker()，否则玩家以外会拿到 null
        LivingEntity attacker = context.getAttacker();
        if (target == null || attacker == null) {
            return damage;
        }

        double armor = target.getAttributeValue(Attributes.ARMOR);

        // 因子 1：目标最大生命比自己高，每高 100 点 +100%，封顶 +100%
        float healthFactor = 1.0f;
        if (target.getMaxHealth() > attacker.getMaxHealth()) {
            healthFactor = (float) (1 + Math.min((target.getMaxHealth() - attacker.getMaxHealth()) / 100.0, 1.0));
        }

        // 因子 2：目标护甲，按护甲值开方增长，封顶 +50%
        float armorFactor = 1.0f;
        if (armor > 0) {
            armorFactor = (float) (1 + Math.min(Math.sqrt(armor) * 0.05, 0.5));
        }

        // 因子 3：每级 +20%
        float levelFactor = 1.0f + modifier.getLevel() * 0.2f;

        return damage * healthFactor * armorFactor * levelFactor;
    }

}
