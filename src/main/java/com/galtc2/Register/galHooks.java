package com.galtc2.Register;

import com.galtc2.Galtc2;
import com.galtc2.Hook.LivingEventModifierHook;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.registry.IdAwareComponentRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHook;

import static com.galtc2.Galtc2.MODID;

public class galHooks {
    public static final IdAwareComponentRegistry<ModuleHook<?>> loader =
            new IdAwareComponentRegistry<>("gal_hooks");
    public static final ModuleHook<LivingEventModifierHook> LIVING_EVENT =
            ModifierHooks.register(new ResourceLocation(MODID,"livingevent"),LivingEventModifierHook.class,LivingEventModifierHook.AllMerger::new,new LivingEventModifierHook(){});


    public static void register() {
    }
}

