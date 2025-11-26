package com.galtc2.Register;

import com.galtc2.Galtc2;
import com.galtc2.content.effect.sakura;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class galtc2Effects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Galtc2.MODID);

    public static final RegistryObject<MobEffect> sakura = EFFECTS.register("sakura",sakura::new);

}
