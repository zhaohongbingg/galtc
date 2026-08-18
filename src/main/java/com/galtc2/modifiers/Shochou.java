package com.galtc2.modifiers;

import com.galtc2.GaltcConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Shochou extends Modifier implements MeleeHitModifierHook {

    // 上次掉落时间记录（玩家UUID -> 游戏时间）
    private static final java.util.Map<java.util.UUID, Long> LAST_DROP_TIME = new java.util.concurrent.ConcurrentHashMap<>();
    
    // 冷却时间（2秒 = 40 ticks）
    private static final long COOLDOWN_TICKS = 40;

    public Shochou() {}

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (damageDealt <= 0) return;

        LivingEntity attackerLiving = context.getAttacker();
        if (attackerLiving == null || attackerLiving.level().isClientSide) return;
        if (!(attackerLiving instanceof Player attacker)) return;

        // 检查冷却时间（2秒 = 40 ticks）
        long currentTime = attacker.level().getGameTime();
        Long lastDrop = LAST_DROP_TIME.get(attacker.getUUID());
        if (lastDrop != null && (currentTime - lastDrop) < COOLDOWN_TICKS) {
            return; // 还在冷却中
        }

        // 概率判定
        double chance = GaltcConfig.COMMON.SHOCHOU_DROP_CHANCE.get();
        if (attacker.getRandom().nextDouble() >= chance) return;

        // 获取配置的单次掉落数量上限，并在 1~上限 之间随机掉落
        int dropMax = GaltcConfig.COMMON.SHOCHOU_DROP_AMOUNT.get();
        if (dropMax <= 0) return;
        int dropCount = 1 + attacker.getRandom().nextInt(dropMax);

        ResourceLocation itemLoc = ResourceLocation.tryParse(GaltcConfig.COMMON.SHOCHOU_MONEY_ITEM.get());
        if (itemLoc == null) return;
        Item moneyItem = ForgeRegistries.ITEMS.getValue(itemLoc);
        if (moneyItem == null || moneyItem == Items.AIR) return;

        ItemStack dropStack = new ItemStack(moneyItem, dropCount);

        // 在目标位置掉落
        LivingEntity target = context.getLivingTarget();
        if (target == null) return;

        Level level = attacker.level();
        ItemEntity itemEntity = new ItemEntity(level, target.getX(), target.getY(), target.getZ(), dropStack);
        level.addFreshEntity(itemEntity);

        // 更新上次掉落时间
        LAST_DROP_TIME.put(attacker.getUUID(), currentTime);
    }
}
