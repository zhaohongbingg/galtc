package com.galtc2.Register;

import com.galtc2.Galtc2;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import slimeknights.mantle.registration.deferred.BlockDeferredRegister;
import slimeknights.mantle.registration.object.MetalItemObject;

import java.util.List;
import java.util.function.Function;

/**
 * 材料金属的注册位：每种材料一次拿到「金属块 / 锭 / 粒」三件套。
 * <p>
 * 走的是匠魂官方的 {@link BlockDeferredRegister#registerMetal}，它内部会把三条 id 一起注册：
 * <pre>
 *   galtc2:&lt;名&gt;_block    金属块（带 BlockItem）
 *   galtc2:&lt;名&gt;_ingot    锭
 *   galtc2:&lt;名&gt;_nugget   粒
 * </pre>
 * 后缀由 Mantle 写死，没有重载能改 —— 所以这套 id 与改造前的裸锭
 * （{@code galtc2:<名>}）<b>不是同一批物品</b>，旧存档里的锭会变成未知物品。
 * <p>
 * 与匠魂自身的 {@code TinkerMaterials} 写法保持一致，但有两处不能照抄：
 * <ul>
 *   <li>{@code TinkerMaterials.BLOCKS} 是 TConstruct 自己的 {@code protected} 实例，外部模组拿不到，
 *       所以要在这里新建一个属于 {@code galtc2} 的 {@link BlockDeferredRegister}；</li>
 *   <li>{@code metalBuilder(...)} / {@code BLOCK_ITEM} / {@code ITEM_PROPS} 都属于 TConstruct 的
 *       {@code TinkerModule}，且全是 {@code protected static}，因此下面按它们的原始实现复刻了一份。</li>
 * </ul>
 * 注意 {@link MetalItemObject#values()} 在 Mantle 1.11.79 里有个笔误（第三个元素重复了锭、漏了粒），
 * 要遍历请用 {@link MetalItemObject#forEach} 或直接取三个 getter。
 */
public class galtcMetal {

    /**
     * 本模组的金属注册器。{@link BlockDeferredRegister} 自带 block 与 item 两个注册表，
     * 在 {@link Galtc2} 构造函数里 {@code register(modEventBus)} 一次即可注册两边。
     */
    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(Galtc2.MODID);

    // 下面三个成员与 TConstruct TinkerModule 里的同名成员一一对应，
    // 做完 galtc2 主材料后被 jvt 的 JvtMetal 复用，所以保持 public。

    /** 与 {@code TinkerModule#ITEM_PROPS} 等价：三件套共用的物品属性模板。 */
    public static final Item.Properties ITEM_PROPS = new Item.Properties();

    /** 与 {@code TinkerModule#BLOCK_ITEM} 等价：金属块的 BlockItem 工厂。 */
    public static final Function<Block, ? extends BlockItem> BLOCK_ITEM =
            block -> new BlockItem(block, ITEM_PROPS);

    /**
     * 与 {@code TinkerModule#metalBuilder} 等价：
     * 金属音效 + 铁琴音高 + 需要正确工具 + 硬度 5.0，和匠魂自家金属块手感一致。
     */
    public static BlockBehaviour.Properties metalBuilder(MapColor color) {
        return BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .mapColor(color)
                .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                .requiresCorrectToolForDrops()
                .strength(5.0F);
    }

    // ---------------------------------------------------------------- 材料金属
    // 顺序与 materials.csv / 原 galtcItem 的注册顺序保持一致。
    // MapColor 取自各自熔融流体的方块颜色（galtc2Fluid / JerotesFluids），只影响地图上的呈现。

    /** SPRB（漆黑） */
    public static final MetalItemObject sprb =
            BLOCKS.registerMetal("sprb", metalBuilder(MapColor.COLOR_BLACK), BLOCK_ITEM, ITEM_PROPS);
    /** 零一（金属灰） */
    public static final MetalItemObject zeroone =
            BLOCKS.registerMetal("zeroone", metalBuilder(MapColor.METAL), BLOCK_ITEM, ITEM_PROPS);
    /** 笼女（珠光白） */
    public static final MetalItemObject kagome =
            BLOCKS.registerMetal("kagome", metalBuilder(MapColor.DIAMOND), BLOCK_ITEM, ITEM_PROPS);
    /** 苍彼（天青） */
    public static final MetalItemObject aosora =
            BLOCKS.registerMetal("aosora", metalBuilder(MapColor.COLOR_LIGHT_BLUE), BLOCK_ITEM, ITEM_PROPS);
    /** ATRI（米白） */
    public static final MetalItemObject atri =
            BLOCKS.registerMetal("atri", metalBuilder(MapColor.COLOR_PURPLE), BLOCK_ITEM, ITEM_PROPS);
    /** 艾佩莉雅（奶油） */
    public static final MetalItemObject apeiria =
            BLOCKS.registerMetal("apeiria", metalBuilder(MapColor.COLOR_MAGENTA), BLOCK_ITEM, ITEM_PROPS);
    /** 水仙（淡紫） */
    public static final MetalItemObject narcissus =
            BLOCKS.registerMetal("narcissus", metalBuilder(MapColor.COLOR_YELLOW), BLOCK_ITEM, ITEM_PROPS);
    /** CA / 水葬（金） */
    public static final MetalItemObject ca =
            BLOCKS.registerMetal("ca", metalBuilder(MapColor.COLOR_YELLOW), BLOCK_ITEM, ITEM_PROPS);
    /** 命运石之门（橙） */
    public static final MetalItemObject steinsgate =
            BLOCKS.registerMetal("steinsgate", metalBuilder(MapColor.COLOR_YELLOW), BLOCK_ITEM, ITEM_PROPS);
    /** 海豹（天蓝） */
    public static final MetalItemObject azarashi =
            BLOCKS.registerMetal("azarashi", metalBuilder(MapColor.COLOR_YELLOW), BLOCK_ITEM, ITEM_PROPS);
    /** 夜时羊（深蓝） */
    public static final MetalItemObject nighttimesheep =
            BLOCKS.registerMetal("nighttimesheep", metalBuilder(MapColor.COLOR_YELLOW), BLOCK_ITEM, ITEM_PROPS);
    /** 生命的备件（粉） */
    public static final MetalItemObject oumunnbyou =
            BLOCKS.registerMetal("oumunnbyou", metalBuilder(MapColor.COLOR_YELLOW), BLOCK_ITEM, ITEM_PROPS);
    /** 入木三分（珊瑚红） */
    public static final MetalItemObject kisaragikurenai =
            BLOCKS.registerMetal("kisaragikurenai", metalBuilder(MapColor.COLOR_YELLOW), BLOCK_ITEM, ITEM_PROPS);
    /** 黑百合（黑） */
    public static final MetalItemObject blacklily =
            BLOCKS.registerMetal("blacklily", metalBuilder(MapColor.COLOR_YELLOW), BLOCK_ITEM, ITEM_PROPS);
    /** 村正（暗红） */
    public static final MetalItemObject muramasa =
            BLOCKS.registerMetal("muramasa", metalBuilder(MapColor.COLOR_YELLOW), BLOCK_ITEM, ITEM_PROPS);
    /** 所长（棕） */
    public static final MetalItemObject shochou =
            BLOCKS.registerMetal("shochou", metalBuilder(MapColor.COLOR_BROWN), BLOCK_ITEM, ITEM_PROPS);
    /** 泉ひより（明黄） */
    public static final MetalItemObject izumi_hiyori =
            BLOCKS.registerMetal("izumi_hiyori", metalBuilder(MapColor.COLOR_YELLOW), BLOCK_ITEM, ITEM_PROPS);
    /** 锦明日海（淡青） */
    public static final MetalItemObject nishiki_asumi =
            BLOCKS.registerMetal("nishiki_asumi", metalBuilder(MapColor.COLOR_ORANGE), BLOCK_ITEM, ITEM_PROPS);
    /** 奇恩异典（灰蓝） */
    public static final MetalItemObject qienidian =
            BLOCKS.registerMetal("qienidian", metalBuilder(MapColor.COLOR_LIGHT_BLUE), BLOCK_ITEM, ITEM_PROPS);
    /** 罚抄（浅橙） */
    public static final MetalItemObject rewrite =
            BLOCKS.registerMetal("rewrite", metalBuilder(MapColor.COLOR_ORANGE), BLOCK_ITEM, ITEM_PROPS);
    /** 坏女人（粉） */
    public static final MetalItemObject badwoman =
            BLOCKS.registerMetal("badwoman", metalBuilder(MapColor.COLOR_PINK), BLOCK_ITEM, ITEM_PROPS);
    /** 银河铁道之夜（深蓝） */
    public static final MetalItemObject galaxy_railroad =
            BLOCKS.registerMetal("galaxy_railroad", metalBuilder(MapColor.COLOR_BLUE), BLOCK_ITEM, ITEM_PROPS);
    /** Ciallo（草绿） */
    public static final MetalItemObject ciallo =
            BLOCKS.registerMetal("ciallo", metalBuilder(MapColor.COLOR_GREEN), BLOCK_ITEM, ITEM_PROPS);
    /** 月社妃（米黄） */
    public static final MetalItemObject tsukiyashiro_kisaki =
            BLOCKS.registerMetal("tsukiyashiro_kisaki", metalBuilder(MapColor.COLOR_YELLOW), BLOCK_ITEM, ITEM_PROPS);
    /** 沙耶（深绿，J村 联动） */
    public static final MetalItemObject saya =
            BLOCKS.registerMetal("saya", metalBuilder(MapColor.COLOR_GREEN), BLOCK_ITEM, ITEM_PROPS);
    /** 勺景（近黑紫，J村 联动） */
    public static final MetalItemObject shoujing =
            BLOCKS.registerMetal("shoujing", metalBuilder(MapColor.COLOR_PURPLE), BLOCK_ITEM, ITEM_PROPS);
    /** 腐姬（血红，J村 联动） */
    public static final MetalItemObject euthanasia =
            BLOCKS.registerMetal("euthanasia", metalBuilder(MapColor.COLOR_RED), BLOCK_ITEM, ITEM_PROPS);
    /** 清枫（灰，J村 联动） */
    public static final MetalItemObject qingfeng =
            BLOCKS.registerMetal("qingfeng", metalBuilder(MapColor.COLOR_GRAY), BLOCK_ITEM, ITEM_PROPS);
    /** 莫尔加那（暗紫，J村 联动） */
    public static final MetalItemObject morgana =
            BLOCKS.registerMetal("morgana", metalBuilder(MapColor.COLOR_PURPLE), BLOCK_ITEM, ITEM_PROPS);

    // 注：万花镜(kaleidoscope) 不在这里。它不是匠魂材料，只作强化材料使用，
    // 因此保持普通物品注册（见 galtcItem#kaleidoscope），没有块与粒。

    /**
     * 创造栏与着色都按这个顺序遍历（与上面字段声明顺序一致）。
     * <p>
     * 不用 {@code List.of(...)} 之外的方式拼接，是为了让「注册顺序 = 这里的顺序」这一点一眼可见。
     */
    public static final List<MetalItemObject> ALL_METALS = List.of(
            sprb, zeroone, kagome, aosora, atri, apeiria, narcissus, ca,
            steinsgate, azarashi, nighttimesheep, oumunnbyou, kisaragikurenai, blacklily,
            muramasa, shochou, izumi_hiyori, nishiki_asumi, qienidian, rewrite,
            badwoman, galaxy_railroad, ciallo, tsukiyashiro_kisaki,
            saya, shoujing, euthanasia, qingfeng, morgana
    );

    public static void register() {
    }
}
