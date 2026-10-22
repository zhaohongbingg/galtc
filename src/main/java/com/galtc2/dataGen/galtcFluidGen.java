package com.galtc2.dataGen;

import com.galtc2.Galtc2;
import com.galtc2.Register.galtc2Fluid;
import com.galtc2.compat.jerotes.JerotesCompat;
import com.galtc2.compat.jerotes.JerotesFluids;
import com.galtc2.jvt.Register.JvtFluid;
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
        this.molten(galtc2Fluid.molten_apeiria, 0xFFfcf7e1);
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
        this.molten(galtc2Fluid.molten_shochou, 0xFFcdb29e);
        this.molten(galtc2Fluid.molten_izumi_hiyori, 0xFFFFFF00);
        this.molten(galtc2Fluid.molten_nishiki_asumi, 0xFFD8EFF5);
        this.molten(galtc2Fluid.molten_qienidian, 0xFFB4B9DD);
        this.molten(galtc2Fluid.molten_rewrite, 0xFFE9AD86);
        this.molten(galtc2Fluid.molten_badwoman, 0xFFF9C9D3);
        this.molten(galtc2Fluid.molten_galaxy_railroad, 0xFF0A1128);
        this.molten(galtc2Fluid.molten_ciallo, 0xFFB6CFA5);
        this.molten(galtc2Fluid.molten_tsukiyashiro_kisaki, 0xFFF2E7A1);

        this.molten(galtc2Fluid.molten_juqing, 0xFFD4AF37);
        this.molten(galtc2Fluid.molten_seqi, 0xFFFF69B4);
        this.molten(galtc2Fluid.molten_feimeng, 0xFF87CEEB);
        this.molten(galtc2Fluid.molten_guro, 0xFF8B0000);

        this.molten(galtc2Fluid.molten_morgana, 0xFF6A4C93);

        this.molten(galtc2Fluid.molten_tianjin_zui, 0xFF9E8A9D);

        // J村 联动材料的流体只在安装了 J村 时才注册，因此这里也要判断：
        // 未安装时它们的 FluidType 不在注册表里，直接取会抛异常。
        // 对应的 4 个 mantle/fluid_texture/molten_*.json 已随代码提交，装了 J村 再跑 runData 会写出同样内容。
        if (JerotesCompat.isJerotesLoaded()) {
            this.molten(JerotesFluids.moltenSaya, 0xFF1F4D2E);
            this.molten(JerotesFluids.moltenShoujing, 0xFF16131F);
            this.molten(JerotesFluids.moltenEuthanasia, 0xFF8B0000);
            this.molten(JerotesFluids.moltenQingfeng, 0xFF9BA3AB);
        }





        // ---- JV Tinkers（原 jvt 模组）的熔融金属 ----
        this.molten(JvtFluid.molten_villager_metal, 0xFFB06F35);
        this.molten(JvtFluid.molten_qoaiku_metal, 0xFF10AB86);
        this.molten(JvtFluid.molten_serpon_alloy, 0xFFD08405);
        this.molten(JvtFluid.molten_meror, 0xFFC47D89);
        this.molten(JvtFluid.molten_refine_meror, 0xFFCA949D);
        this.molten(JvtFluid.molten_ancient_second_rounder, 0xFF5E9BB0);
        this.molten(JvtFluid.molten_fire_apostle, 0xFFE86A2A);
        this.molten(JvtFluid.molten_mountain_realm, 0xFFC9A227);
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
