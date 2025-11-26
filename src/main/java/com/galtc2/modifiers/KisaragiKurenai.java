package com.galtc2.modifiers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.UUID;
import java.util.function.BiConsumer;

public class KisaragiKurenai extends BaseModifiers{
    public KisaragiKurenai() {

    }
    @Override
    public boolean isNoLevels() {
        return false;
    }


    @Override
    public float getMeleeDamage(IToolStackView tool, slimeknights.tconstruct.library.modifiers.ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity enemy=context.getLivingTarget();
        double factor=1+modifier.getLevel()*0.25;
        if(!isSpider(enemy)){
            return (float) (damage*factor);
        }
        return damage;
    }

    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level, Projectile projectile, AbstractArrow arrow, EntityHitResult hit, LivingEntity attacker, LivingEntity target) {
        double factor=1+level*0.25;
         if(!isSpider(target)){
            arrow.setBaseDamage(arrow.getBaseDamage()*factor);
         }
    }

    private static boolean isSpider(LivingEntity entity){
        return entity instanceof Spider||
                entity instanceof CaveSpider;

    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Attribute, AttributeModifier> consumer) {
         double extraHealth=5+modifier.getLevel();
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            consumer.accept(
                    Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            UUID.fromString("c9c9c9c9-c9c9-c9c9-c9c9-c9c9c9c9c9c9"),
                            "tconstruct.kisaragi_kurenai.health",
                            extraHealth,
                            AttributeModifier.Operation.ADDITION
                    )
            );
        }

    }
}
