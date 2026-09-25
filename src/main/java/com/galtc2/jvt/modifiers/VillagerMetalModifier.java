package com.galtc2.jvt.modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.phys.AABB;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 村民金属·特性(村民之力)
 * 附近每只村民提升 5% 攻击(近战伤害)与 5% 防御(受到伤害减免)。
 * 采用按需检测：只在攻击结算/受伤结算时统计附近村民，不做每 tick 扫描。
 */
public class VillagerMetalModifier extends Modifier implements MeleeDamageModifierHook, ModifyDamageModifierHook {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };
    /** 检测半径(格) */
    private static final double RADIUS = 16.0;
    /** 每只村民的加成 */
    private static final float BONUS_PER_VILLAGER = 0.05f;
    /** 计入的村民上限(避免村庄刷屏时数值爆炸) */
    private static final int MAX_VILLAGERS = 16;
    /** 防御减伤封顶 */
    private static final float MAX_DEFENSE_REDUCTION = 0.8f;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.MODIFY_HURT);
    }

    /** 攻击：加算 +5%/村民 */
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        int villagers = countVillagers(context.getAttacker());
        if (villagers <= 0) {
            return damage;
        }
        return damage * (1.0f + BONUS_PER_VILLAGER * villagers);
    }

    /** 防御：减伤 5%/村民，封顶 80%，只结算一次(与穿戴件数无关) */
    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0) {
            return amount;
        }
        EquipmentSlot firstSlot = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(this) > 0) {
                firstSlot = slot;
                break;
            }
        }
        if (firstSlot == null || slotType != firstSlot) {
            return amount;
        }
        int villagers = countVillagers(context.getEntity());
        if (villagers <= 0) {
            return amount;
        }
        float reduction = Math.min(BONUS_PER_VILLAGER * villagers, MAX_DEFENSE_REDUCTION);
        return amount * (1.0f - reduction);
    }

    /** 统计附近存活的村民(含流浪商人这类 AbstractVillager)，按需调用 */
    private static int countVillagers(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) {
            return 0;
        }
        AABB area = entity.getBoundingBox().inflate(RADIUS);
        int count = entity.level().getEntitiesOfClass(AbstractVillager.class, area, v -> v.isAlive() && !v.isRemoved()).size();
        return Math.min(count, MAX_VILLAGERS);
    }
}
