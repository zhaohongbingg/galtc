package com.galtc2.Register;

import com.galtc2.Galtc2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class galtc2Sounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Galtc2.MODID);

    public static final RegistryObject<SoundEvent> CIALLO =
            SOUND_EVENTS.register("ciallo.ciallo",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Galtc2.MODID, "ciallo.ciallo")));
}
