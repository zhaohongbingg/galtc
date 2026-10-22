# galtc2

Minecraft 1.20.1 / Forge 47 的匠魂（Tinkers' Construct）附属模组：新增一批金属材料、修饰器，以及
J村 / 迎战 / 祸乱鬼魅 / 遗境守望 的软联动内容。

- 材料清单：`materials.csv`（Excel 版 `materials.xlsx`）
- 新增材料的完整步骤：`.trae/skills/tcon-material-helper/SKILL.md`
- 常用命令：`gradlew runClient`、`gradlew runData`（数据生成）、`gradlew build`

## 金属材料的配方体系

每种金属都有 `galtc2:<名>_block` / `_ingot` / `_nugget` 三件套，由 `com.galtc2.Register.galtcMetal`
和 `com.galtc2.jvt.Register.JvtMetal` 通过 Mantle 的 `registerMetal` 注册。

对应配方放在 `src/main/resources/data/galtc2/recipes/<名>/`，共 15 个文件：

| 文件 | 作用 | 类型 |
|---|---|---|
| `<名>_melting_ingot.json` | 熔炼锭 → 90 mB | `tconstruct:melting` |
| `<名>_melting_block.json` | 熔炼块 → 810 mB | `tconstruct:melting` |
| `<名>_melting_nugget.json` | 熔炼粒 → 10 mB | `tconstruct:melting` |
| `<名>_casting_block.json` | 浇铸块（铸造盆 810 mB） | `tconstruct:casting_basin` |
| `<名>_casting_ingot_single.json` | 单次铸锭（一次性铸模） | `tconstruct:casting_table` |
| `<名>_casting_ingot_multi.json` | 多次铸锭（金铸模） | `tconstruct:casting_table` |
| `<名>_casting_nugget_single.json` | 单次铸粒（一次性铸模，10 mB） | `tconstruct:casting_table` |
| `<名>_casting_nugget_multi.json` | 多次铸粒（金铸模，10 mB） | `tconstruct:casting_table` |
| `<名>_block_from_ingots.json` | 9 锭 → 1 块 | `minecraft:crafting_shaped` |
| `<名>_ingot_from_block.json` | 1 块 → 9 锭 | `minecraft:crafting_shapeless` |
| `<名>_ingot_from_nuggets.json` | 9 粒 → 1 锭 | `minecraft:crafting_shaped` |
| `<名>_nugget_from_ingot.json` | 1 锭 → 9 粒 | `minecraft:crafting_shapeless` |
| `<名>_material_melt.json` | 匠魂材料系统：材料熔炼 | `tconstruct:material_melting` |
| `<名>_material_cast.json` | 匠魂材料系统：材料浇铸 | `tconstruct:material_fluid` |
| `<名>_material11.json` | 匠魂材料系统：1 锭 = 1 材料值 | `tconstruct:material` |

### 数值约定

| 单位 | 流体量 | 时间 |
|---|---|---|
| 锭 | 90 mB | 熔炼 72 tick / 冷却 72 tick |
| 块 | 810 mB（= 9 锭） | 熔炼 194 tick / 冷却 194 tick |
| 粒 | 10 mB（= 1/9 锭） | 熔炼 22 tick / 冷却 20 tick |

温度不写死：从该材料已有的 `<名>_melting_ingot.json` 里取 `temperature`（各材料不同，
例如 sprb 1500、ancient_second_rounder 1450），新加材料时照抄同目录的锭熔炼值即可。

### 为什么写物品 id 而不是 forge 标签

galtc2 自己的锭/块/粒**没有**生成 `forge:ingots|nuggets|storage_blocks/<名>` 标签（项目里没有物品标签
datagen），所以转化与熔融配方直接写 `{"item": "galtc2:<名>_ingot"}`。若想改成标签写法，必须先把标签
补出来（`data/forge/tags/items/...`），否则标签为空会让配方静默失效。

例外是 J村 的 5 个联动材料（`meror_metal`、`qoaiku_metal`、`refine_meror_metal`、`serpon_alloy`、
`villager_metal`）：它们的锭属于 J村，标签由 J村 提供，那套配方用的是 `forge:*` 标签。

### 联动金属

`ancient_second_rounder` / `fire_apostle` / `mountain_realm` 三个金属只在装了 J村 时注册，
新加的 7 个配方（块/粒熔炼、块浇铸、4 个工作台转化）都带
`"conditions": [{ "type": "forge:mod_loaded", "modid": "jerotesvillage" }]`，没装 J村 时整条配方不会加载。

J村 自己的 5 个联动材料（`meror` / `qoaiku_metal` / `refine_meror` / `serpon_alloy` / `villager_metal`）
的锭、块、粒转化由 J村 自己提供，galtc2 这边只补了熔炼：块熔炼（810 mB、`time` 648）与粒熔炼
（10 mB、`time` 8，只有 J村 有对应粒标签的 `meror_metal` / `qoaiku_metal` / `serpon_alloy` 三个）。

### 粒的浇铸

有粒物品的 33 个材料各补了两条配方（`tconstruct:casting_table` + `tconstruct:casts/*/nugget`，
10 mB、`cooling_time` 20），写法与匠魂原版 `casting/metal/<名>/nugget_gold_cast.json` 一致：

- `_casting_nugget_multi.json`：多用途（金）铸模，不消耗。
- `_casting_nugget_single.json`：一次性（砂）铸模，`cast_consumed: true`。

`ancient_second_rounder` / `fire_apostle` / `mountain_realm` 的粒只在装了 J村 时注册，这三套配方
带 `forge:mod_loaded` 条件。J村 自己的 5 个联动材料粒不属于 galtc2，故不生成。

### 暂未覆盖

- 块 / 粒 在匠魂材料系统里的材料值（`tconstruct:material` 的 block / nugget 变体）

## 流体显示（多少块 / 多少锭）

炉子 / 桶的 tooltip 里“= N 块 / N 锭 / N 粒”由 Mantle 的 `FluidTooltipHandler` 负责，走两层流体标签：

```
流体 → #forge:molten_<名>（data/forge/tags/fluids/）→ tconstruct:tooltips/metal
     （data/tconstruct/tags/fluids/tooltips/metal.json）→ 单位表
     assets/tconstruct/mantle/fluid_tooltips/metals.json
```

单位表内容（匠魂自带，本模组直接复用）：

| 单位 | 翻译键 | 需要 mB |
|---|---|---|
| 块 | `gui.tconstruct.fluid.block` | 810 |
| 锭 | `gui.tconstruct.fluid.ingot` | 90 |
| 粒 | `gui.tconstruct.fluid.nugget` | 10 |

本模组的做法（A 方案，不动匠魂的单位表）：

1. `data/forge/tags/fluids/molten_<名>.json` —— 每个熔融流体一个，`values` 里放 `galtc2:molten_<名>`（共 42 个）。
2. `data/tconstruct/tags/fluids/tooltips/metal.json` —— `"replace": false`，只追加 `#forge:molten_<名>`（38 个金属流体）。
   不同数据包的同名流体标签会合并，所以匠魂原有的金属条目不受影响。

`molten_feimeng` / `molten_guro` / `molten_juqing` / `molten_seqi` 是碎片类流体，不是「块/锭/粒」体系，
只建了 `forge:molten_*` 标签，没有进 `tooltips/metal`（否则会算出“几块几锭”，没有意义）。

## 方块标签

带 `requiresCorrectToolForDrops()` 的方块必须出现在
`src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json`（该文件手写维护，datagen 不再生成），
否则镐子挖得动但不掉落。只有装联动模组才注册的方块用 `{ "id": "...", "required": false }` 标记。
