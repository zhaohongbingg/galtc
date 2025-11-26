package com.galtc2.dataGen;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtc2Fluid;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.fluid.texture.AbstractFluidTextureProvider;
import slimeknights.mantle.fluid.texture.FluidTexture;
import slimeknights.mantle.registration.object.FluidObject;

public class galtcFluidGen extends AbstractFluidTextureProvider {
    public galtcFluidGen(PackOutput packOutput, @Nullable ExistingFileHelper existingFileHelper) {
        super(packOutput, "galtc2");
    }


    @Override
    public void addTextures() {
        this.molten(galtc2Fluid.molten_kagome, 0xFFF8F8FF);
        this.molten(galtc2Fluid.molten_aosora, 0xFF87CEFA);
        this.molten(galtc2Fluid.molten_kagari, 0xFFe6aefc);
        this.molten(galtc2Fluid.molten_apeiria, 0xFFfcf7e1);
        this.molten(galtc2Fluid.molten_einstein, 0xFFF5F5DC);
        this.molten(galtc2Fluid.molten_narcissus, 0xFFBF9DFD);
        this.molten(galtc2Fluid.molten_ca, 0xFFFFD700);
        this.molten(galtc2Fluid.molten_atri,0xFFfce6d8);
        this.molten(galtc2Fluid.molten_zeroone,0xFFa08e5f);
        this.molten(galtc2Fluid.molten_sprb,0xFF00bfff);
        this.molten(galtc2Fluid.molten_steinsgate, 0xFFFFA500); // 橙色（示例）
        this.molten(galtc2Fluid.molten_azarashi, 0xFF87CEEB); // 天蓝（示例）
        this.molten(galtc2Fluid.molten_nighttimesheep, 0xFF2F4F4F); // 深灰（示例）
        this.molten(galtc2Fluid.molten_oumunnbyou, 0xFFffb7c5); // 紫色（示例）
        this.molten(galtc2Fluid.molten_kisaragikurenai, 0xFFff6961); // 橘红（示例）
        this.molten(galtc2Fluid.molten_blacklily, 0xFF000000); // 黑色（示例）
        this.molten(galtc2Fluid.molten_muramasa,  0xFF660000);




    }
    public FluidTexture.Builder texture(FluidObject<?> fluid, String id) {
        return this.texture(fluid).textures(Galtc2.loc("fluid/material/" + id), false, false);
    }

    public FluidTexture.Builder molten(FluidObject<?> fluid, int color) {
        return this.texture(fluid).color(color).textures(Galtc2.loc("fluid/molten/"), false, false);
    }

    public FluidTexture.Builder liquid(FluidObject<?> fluid, int color) {
        return this.texture(fluid).color(color).textures(Galtc2.loc("fluid/liquid/"), false, false);
    }




    @Override
    public String getName() {
        return"galtc2";
    }
}
