//package com.galtc2.dataGen.material;
//
//import com.galtc2.Register.galtcItem;
//import com.galtc2.content.MaterialDefData;
//import net.minecraft.data.PackOutput;
//import slimeknights.tconstruct.library.data.material.AbstractMaterialDataProvider;
//
//public class galtcMaterialDefGen extends AbstractMaterialDataProvider {
//
//    public galtcMaterialDefGen(PackOutput packOutput) {
//        super(packOutput);
//    }
//
//    @Override
//    protected void addMaterials() {
//        for(galtcItem.ALL_ITEMS:){
//            MaterialDefData def=mate.holder.defintion();
//            if(def!=null){
//                this.addMaterial(def.getMate());
//            }
//        }
//
//    }
//
//    @Override
//    public String getName() {
//        return null;
//    }
//}
