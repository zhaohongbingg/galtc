package modifiers;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.data.tinkering.AbstractModifierProvider;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.modules.build.ModifierSlotModule;
import slimeknights.tconstruct.library.tools.SlotType;

import static slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial.ARMOR_SLOTS;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

public class SlotModifiers extends AbstractModifierProvider implements IConditionBuilder {
    public SlotModifiers(PackOutput packOutput){
        super(packOutput);
    }

    protected void addModifiers() {
        EquipmentSlot[] handSlots = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
        EquipmentSlot[] armorSlots = ARMOR_SLOTS;
        EquipmentSlot[] armorMainHand = {EquipmentSlot.MAINHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};


        ModifierSlotModule UPGRADE = ModifierSlotModule.slot(SlotType.UPGRADE).eachLevel(1);
        buildModifier(ModifierDatas.SPRB.getId()).priority(60).addModule(UPGRADE);


    }


    @Override
    public String getName() {
        return "Tinkers' Construct Modifiers";
    }

    /** Short helper to get a modifier ID */
    private static ModifierId id(String name) {
        return new ModifierId(TConstruct.MOD_ID, name);
    }

    /** Short helper to get a modifier ID */
    private static Pattern pattern(String name) {
        return new Pattern(TConstruct.MOD_ID, name);
    }
}
