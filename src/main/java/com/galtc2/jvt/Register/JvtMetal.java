package com.galtc2.jvt.Register;

import com.galtc2.Register.galtcMetal;
import com.galtc2.jvt.Jvt;
import net.minecraft.world.level.material.MapColor;
import slimeknights.mantle.registration.deferred.BlockDeferredRegister;
import slimeknights.mantle.registration.object.MetalItemObject;

import java.util.List;

/**
 * JV Tinkers 自有金属的注册位：金属块 / 锭 / 粒三件套。
 * <p>
 * 走的是和 {@link galtcMetal} 同一套匠魂官方写法（{@code BlockDeferredRegister#registerMetal}），
 * 用具工厂直接复用 {@link galtcMetal#metalBuilder} / {@link galtcMetal#BLOCK_ITEM} /
 * {@link galtcMetal#ITEM_PROPS}，不再重复一份。
 * <p>
 * 与 galtc2 主材料的一个区别：这 3 种金属的<b>锭本来就是独立彩色贴图</b>
 * （{@code item/<名>_ingot}，而不是共享灰度 + tintindex 着色），
 * 所以它们的块与粒也各配一张独立贴图（{@code block/<名>_block}、{@code item/<名>_nugget}），
 * 不走 {@code IngotItemColor} / {@code BlockColor} 那套着色。
 * 粒的高光层仍然复用共享的 {@code item/nugget_highlight}。
 * <p>
 * 另外这里的注册是有条件的 —— 整个 JV 部分都建立在 J村 之上，
 * {@link Jvt#init} 在未安装 J村 时会整段跳过，参见 {@link Jvt} 的类注释。
 */
public final class JvtMetal {

    /** 本模块的金属注册器（block 与 item 两张表由它一并管理）。 */
    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(Jvt.MODID);

    /** 古二轮金属（铁 + 钴合金，青蓝） */
    public static final MetalItemObject ancient_second_rounder =
            BLOCKS.registerMetal("ancient_second_rounder", galtcMetal.metalBuilder(MapColor.COLOR_CYAN),
                    galtcMetal.BLOCK_ITEM, galtcMetal.ITEM_PROPS);
    /** 火焰使徒金属（橙） */
    public static final MetalItemObject fire_apostle =
            BLOCKS.registerMetal("fire_apostle", galtcMetal.metalBuilder(MapColor.COLOR_ORANGE),
                    galtcMetal.BLOCK_ITEM, galtcMetal.ITEM_PROPS);
    /** 镇山金属（土黄） */
    public static final MetalItemObject mountain_realm =
            BLOCKS.registerMetal("mountain_realm", galtcMetal.metalBuilder(MapColor.TERRACOTTA_ORANGE),
                    galtcMetal.BLOCK_ITEM, galtcMetal.ITEM_PROPS);

    /** 创造栏按这个顺序遍历。 */
    public static final List<MetalItemObject> ALL_METALS = List.of(
            ancient_second_rounder, fire_apostle, mountain_realm
    );

    private JvtMetal() {
    }

    public static void register() {
    }
}
