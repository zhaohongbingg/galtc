package com.galtc2.content.block;

import com.galtc2.Register.galtcBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Gal 母岩：照着匠魂的史莱姆水晶母岩 / 原版紫水晶母岩那套做。
 * <p>
 * 随机刻（{@code 1/5} 概率）时会挑一个相邻方向，把空位长成小晶芽，
 * 已有晶芽则按 <b>小晶芽 → 中晶芽 → 大晶芽 → Gal 晶簇</b> 逐级长大（朝向必须一致），
 * 长成晶簇后就不再变化，和原版紫水晶完全一致（包括写回 facing / waterlogged 和铛的一声）。
 */
public class BuddingGalCoreBlock extends BuddingAmethystBlock {

    private static final Direction[] DIRECTIONS = Direction.values();

    public BuddingGalCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        Direction direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        BlockPos target = pos.relative(direction);
        BlockState targetState = level.getBlockState(target);

        Block next = null;
        if (BuddingAmethystBlock.canClusterGrowAtState(targetState)) {
            next = galtcBlock.SMALL_GAL_CORE_BUD.get();
        } else if (targetState.is(galtcBlock.SMALL_GAL_CORE_BUD.get())
                && targetState.getValue(AmethystClusterBlock.FACING) == direction) {
            next = galtcBlock.MEDIUM_GAL_CORE_BUD.get();
        } else if (targetState.is(galtcBlock.MEDIUM_GAL_CORE_BUD.get())
                && targetState.getValue(AmethystClusterBlock.FACING) == direction) {
            next = galtcBlock.LARGE_GAL_CORE_BUD.get();
        } else if (targetState.is(galtcBlock.LARGE_GAL_CORE_BUD.get())
                && targetState.getValue(AmethystClusterBlock.FACING) == direction) {
            next = galtcBlock.GAL_CORE.get();
        }
        if (next == null) {
            return;
        }
        level.setBlockAndUpdate(target, next.defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, direction)
                .setValue(AmethystClusterBlock.WATERLOGGED, targetState.getFluidState().is(Fluids.WATER)));
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
