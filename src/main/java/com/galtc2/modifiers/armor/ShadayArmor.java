package com.galtc2.modifiers.armor;

import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 龙背上的骑兵 (shaday_armor) —— 阴影材料的护甲侧特性（龙系）。
 * <p>
 * <b>骑乘天境龙时</b>：受到的伤害先减免 {@value #RIDING_REDUCTION} × 100%（50%），
 * 剩下的那一半<b>由胯下的龙承担</b>（直接对载具实体结算同源伤害）。
 * 四件护甲都带这条时只让「第一件」结算，避免一次受击减免四次。
 * <p>
 * 「骑乘龙」按载具实体类型的命名空间判断（{@code aether_dragon}），
 * 所以没装天境巨龙时这条特性自然不生效，也不会引用任何它的类。
 */
public class ShadayArmor extends BaseModifiers {

    /** 天境巨龙的命名空间 */
    private static final String DRAGON_NAMESPACE = "aether_dragon";
    /** 骑龙时的减伤比例：0.5 = 先减免 50% */
    private static final float RIDING_REDUCTION = 0.5F;

    /** 参与「第一件」判定的护甲槽位 */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public ShadayArmor() {
    }

    @Override
    public float onModifyTakeDamage(IToolStackView tool, ModifierEntry modifier, EquipmentContext context,
                                    EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        LivingEntity holder = context.getEntity();
        if (holder == null || amount <= 0.0F || holder.level().isClientSide()) {
            return amount;
        }
        Entity vehicle = holder.getVehicle();
        if (vehicle == null || !isDragon(vehicle)) {
            return amount;
        }
        // 四件护甲都带时只在「第一件」上结算
        if (!isFirstPiece(context, slotType)) {
            return amount;
        }
        float borneByDragon = amount * RIDING_REDUCTION;
        // 先由龙承担这一半：同源伤害直接砸在载具身上
        vehicle.hurt(source, borneByDragon);
        return amount - borneByDragon;
    }

    /** 载具是不是天境龙（按实体类型命名空间判断，不依赖模组类） */
    private static boolean isDragon(Entity vehicle) {
        return DRAGON_NAMESPACE.equals(EntityType.getKey(vehicle.getType()).getNamespace());
    }

    /** 四件护甲里，按 头→胸→腿→靴 的顺序，只有第一件带这条特性的护甲负责结算 */
    private boolean isFirstPiece(EquipmentContext context, EquipmentSlot slotType) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(this) > 0) {
                return slot == slotType;
            }
        }
        return false;
    }
}
