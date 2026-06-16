package com.galtc2.dataGen;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtcBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public class galtcLootTableProvider extends LootTableProvider {

    public galtcLootTableProvider(PackOutput output) {
        super(output, Set.of(), List.of(
                new SubProviderEntry(galtcBlockLootProvider::new, LootContextParamSets.BLOCK)
        ));
    }
}
