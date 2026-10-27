package com.galtc2.Register;

import com.galtc2.Galtc2;
import com.galtc2.content.galArmorItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;

/**
 * 四部件护甲 —— <b>当前已停用</b>（2026-10-24），代码保留备查。
 *
 * <h2>恢复方法（4 步）</h2>
 * <ol>
 *   <li>取消本类下面整块注释；</li>
 *   <li>取消 {@link galtcItem#ALL_ITEMS} 里 {@code galcArmor.gal_helmet} 那一行的注释；</li>
 *   <li>把下列 5 个文件从 {@code .json.disabled} 改回 {@code .json}：
 *     <pre>
 * data/galtc2/tinkering/tool_definitions/gal_helmet.json
 * data/galtc2/tinkering/station_layouts/gal_helmet.json
 * data/galtc2/recipes/tools/armor/gal_helmet.json
 * data/tconstruct/tags/items/modifiable/armor/helmets.json
 * data/tconstruct/tags/items/modifiable/durability.json</pre></li>
 *   <li>模型 {@code assets/galtc2/models/item/gal_helmet.json} 和 lang
 *       {@code item.galtc2.gal_helmet} 没删，恢复后可直接用。</li>
 * </ol>
 *
 * <h2>当初的实现要点（留着以后看）</h2>
 * <ul>
 *   <li>{@link ModifiableArmorMaterial#create} 只传 {@code HELMET}，会自动建出
 *       {@code ToolDefinition} id = {@code galtc2:gal_helmet}（{@code gal} + {@code _helmet}）；</li>
 *   <li>同一个 id 还要有工匠站布局和 {@code tconstruct:tool_building} 配方，靠
 *       {@code slot_layout} 字段把配方和布局绑起来；</li>
 *   <li>{@code part_stats.parts} 数、{@code default_materials} 数、{@code input_slots} 数三者必须一致；</li>
 *   <li>物品要进 {@code tconstruct:modifiable/armor/helmets} 和 {@code tconstruct:modifiable/durability}
 *       两个标签，否则强化装不上、耐久也不认；</li>
 *   <li>槽位数量是数据驱动的（{@code StationSlotLayoutLoader} 按 {@code inputSlots.size() + 1} 算容器大小），
 *       所以 4 部件<b>不需要 mixin</b>。</li>
 * </ul>
 */
public class galcArmor {

    // ──────────────────────────────────────────────────────────────
    // 四部件护甲：暂时停用，恢复时取消下面这块注释
    //
    //   public static final ModifiableArmorMaterial GAL = ModifiableArmorMaterial.create(
    //           Galtc2.loc("gal"), SoundEvents.ARMOR_EQUIP_IRON, ArmorItem.Type.HELMET);
    //
    //   public static final RegistryObject<Item> gal_helmet = galtcItem.ITEMS.<Item>register(
    //           "gal_helmet",
    //           () -> new galArmorItem(GAL, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    // ──────────────────────────────────────────────────────────────

    private galcArmor() {
    }
}
