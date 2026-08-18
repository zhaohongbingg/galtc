package com.galtc2.Register;

import com.galtc2.Galtc2;
import com.galtc2.content.machine.OreGeneratorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class galtcBlockEntity {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Galtc2.MODID);

    public static final RegistryObject<BlockEntityType<OreGeneratorBlockEntity>> ORE_GENERATOR =
            BLOCK_ENTITIES.register("ore_generator", () ->
                    BlockEntityType.Builder.of(
                            OreGeneratorBlockEntity::new,
                            galtcBlock.ORE_GENERATOR.get()
                    ).build(null)
            );

    public static void register() {
    }
}
