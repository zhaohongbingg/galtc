package com.galtc2.modifiers;

import com.galtc2.GaltcConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

public class StarryTravelModifier extends Modifier implements MeleeDamageModifierHook, TooltipModifierHook {

    public StarryTravelModifier() {}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.TOOLTIP);
    }

    private int getDistanceInChunks(LivingEntity entity) {
        BlockPos spawnPos = entity.level().getSharedSpawnPos();
        if (spawnPos == null) {
            spawnPos = new BlockPos(0, 64, 0);
        }
        BlockPos entityPos = entity.blockPosition();
        int dx = Math.abs(entityPos.getX() - spawnPos.getX()) / 16;
        int dz = Math.abs(entityPos.getZ() - spawnPos.getZ()) / 16;
        return Math.max(dx, dz);
    }

    public static int getArmorBonus(LivingEntity entity) {
        BlockPos spawnPos = entity.level().getSharedSpawnPos();
        if (spawnPos == null) {
            spawnPos = new BlockPos(0, 64, 0);
        }
        BlockPos entityPos = entity.blockPosition();
        int dx = Math.abs(entityPos.getX() - spawnPos.getX()) / 16;
        int dz = Math.abs(entityPos.getZ() - spawnPos.getZ()) / 16;
        int chunks = Math.max(dx, dz);
        int chunksPerArmor = GaltcConfig.COMMON.STARRY_TRAVEL_CHUNKS_PER_ARMOR.get();
        return chunks / chunksPerArmor;
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if (context.getAttacker() == null || context.getAttacker().level().isClientSide()) return damage;
        int chunks = getDistanceInChunks(context.getAttacker());
        int chunksPerDamage = GaltcConfig.COMMON.STARRY_TRAVEL_CHUNKS_PER_DAMAGE.get();
        float bonus = (float) chunks / chunksPerDamage;
        return damage + bonus;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (player != null) {
            int chunks = getDistanceInChunks(player);
            int chunksPerDamage = GaltcConfig.COMMON.STARRY_TRAVEL_CHUNKS_PER_DAMAGE.get();
            int chunksPerArmor = GaltcConfig.COMMON.STARRY_TRAVEL_CHUNKS_PER_ARMOR.get();
            float damageBonus = (float) chunks / chunksPerDamage;
            int armorBonus = chunks / chunksPerArmor;
            tooltip.add(Component.literal("§d当前距离: " + chunks + " 区块"));
            tooltip.add(Component.literal("§d武器加成: +" + String.format("%.1f", damageBonus) + " 伤害"));
            tooltip.add(Component.literal("§d盔甲加成: +" + armorBonus + " 护甲"));
        }
    }
}
