package com.galtc2.compat.bomd;

import com.galtc2.Galtc2;
import com.galtc2.content.block.ObsidianSpikeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 祸乱鬼魅(BOMD) 联动新增的方块。
 * <p>
 * 只有一个「黑曜石地刺」({@code galtc2:obsidian_spike})，而且它<b>已经停用</b>：
 * 「宝石少女」现在直接用 BOMD 的 {@code obsidilith.RiftBurst}（粒子 + 延迟伤害），
 * 「永恒少女」用 BOMD 的 {@code void_blossom.Spikes}，两者都不再往世界里放真方块。
 * 保留注册只是为了不让老存档里已存在的该方块变成"缺失方块"。
 * <p>
 * 本类只在装了 BOMD 时由 {@link BomdCompat#init} 注册，所以未装 BOMD 时地刺在游戏里不存在。
 * 注意它本身并不引用 BOMD 的类型，具备独立注册的能力，只是没必要在没装 BOMD 时出现。
 */
public final class BomdBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Galtc2.MODID);

    /** 黑曜石地刺：踩上去持续受伤，60 tick 后消失 */
    public static final RegistryObject<Block> OBSIDIAN_SPIKE = BLOCKS.register("obsidian_spike",
            () -> new ObsidianSpikeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .forceSolidOff()
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.DESTROY)));

    private BomdBlocks() {
    }
}
