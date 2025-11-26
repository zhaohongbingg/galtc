package com.galtc2.modifiers;

import com.galtc2.Galtc2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public record AIDefenseRecord(float protect, int immuneDurationSeconds)
        implements ModifyDamageModifierHook {

    public static final ResourceLocation KEY = Galtc2.loc("ai_defense");

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier,
                                   EquipmentContext context, EquipmentSlot slotType,
                                   DamageSource source, float amount, boolean isDirectDamage) {

        CompoundTag tag = tool.getPersistentData().getCompound(KEY);

        String lastDamageId = tag.getString("damage_id");
        long lastHitTime = tag.getLong("last_time");

        String currentId = source.getMsgId();
        long gameTime = context.getEntity().level().getGameTime();
        long immuneTicks = immuneDurationSeconds * 20L;   // 30秒 = 600 tick

        // ✔ 在持续时间内，如果是相同伤害 → 完全免疫
        if (lastDamageId.equals(currentId) && gameTime - lastHitTime <= immuneTicks) {
            return 0f;   // 直接免疫伤害
        }

        // ✔ 否则：更新伤害记录
        tag.putString("damage_id", currentId);
        tag.putLong("last_time", gameTime);
        tool.getPersistentData().put(KEY, tag);

        // ✔ 第一次受到时：正常承受伤害
        return amount;
    }
}

