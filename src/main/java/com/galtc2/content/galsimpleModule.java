package com.galtc2.content;

import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;

import java.util.List;

public interface galsimpleModule extends ModifierModule {
    default boolean chance(double chance) {
        return chance >= TConstruct.RANDOM.nextDouble();
    }

    default boolean test(BlockState block, IJsonPredicate<BlockState> test) {
        return test.equals(BlockPredicate.ANY) || test.matches(block);
    }

    default boolean test(LivingEntity entity, IJsonPredicate<LivingEntity> test) {
        return test.equals(LivingEntityPredicate.ANY) || test.matches(entity);
    }

    @Override
    default List<ModuleHook<?>> getDefaultHooks() {
        return HookProvider.defaultHooks();
    }

    List<ModuleHook<?>> getDefaulHooks();
}
