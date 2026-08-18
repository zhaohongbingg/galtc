package com.galtc2.content.machine;

import com.galtc2.Galtc2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 矿石生成器方块。
 * <p>
 * 消耗 FE 能量，按配置自动生成矿石到上方。
 * 通入能量即可自动工作，无需GUI。
 */
public class OreGeneratorBlock extends BaseEntityBlock {

    /** 是否正在运行（用于模型显示） */
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public OreGeneratorBlock() {
        super(Properties.copy(Blocks.IRON_BLOCK)
                .strength(3.5F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .noOcclusion()
        );
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OreGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof OreGeneratorBlockEntity o) {
                o.tickServer();
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OreGeneratorBlockEntity be) {
            // 显示当前能量信息（类似Mekanism的探头功能）
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§7[矿石生成器] §6能量: §e" + be.getEnergyStored() + " §7/ §e" + be.getMaxEnergy() + " §7FE"
                    )
            );
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§7  进度: §b" + be.getProgress() + "§7/§b" + OreGeneratorConfig.get().getTicksPerOperation()
                    )
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            // 破坏时保留能量（若想丢弃可以在此处理）
            super.onRemove(state, level, pos, newState, moved);
        }
    }
}
