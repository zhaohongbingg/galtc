package modifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

public class Celestial extends BaseModifiers {
    @Override
    public boolean isNoLevels() {
        return true;
    }

    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        System.out.println("Celestial onEquip triggered!"); // 调试信息
        System.out.println("Slot: " + context.getChangedSlot());
        System.out.println("Is armor slot: " + isArmorSlot(context.getChangedSlot()));
        System.out.println("Entity: " + context.getEntity());

        if (isArmorSlot(context.getChangedSlot()) && context.getEntity() instanceof Player player) {
            System.out.println("Enabling flight for: " + player.getName().getString());
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        System.out.println("Celestial onUnequip triggered!"); // 调试信息

        if (isArmorSlot(context.getChangedSlot())) {
            if (context.getEntity() instanceof Player player) {
                System.out.println("Checking if player still has celestial armor...");
                if (!hasCelestialArmor(player))  {
                    if (!player.isCreative() && !player.isSpectator()) {
                        System.out.println("Disabling flight for: " + player.getName().getString());
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                    }
                }
            }
        }
    }

    private boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST ||
                slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    private boolean hasCelestialArmor(Player player) {
        for (ItemStack stack : player.getInventory().armor) {
            if (!stack.isEmpty()) {
                try {
                    IToolStackView armorTool = ToolStack.from(stack);
                    for (ModifierEntry entry : armorTool.getModifiers().getModifiers()) {
                        if (entry.getModifier() == this) {
                            System.out.println("Found celestial armor: " + stack.getDisplayName().getString());
                            return true;
                        }
                    }
                } catch (Exception e) {
                    // 如果不是工具，跳过
                    System.out.println("Not a tool: " + stack.getDisplayName().getString());
                }
            }
        }
        System.out.println("No celestial armor found");
        return false;
    }
}
