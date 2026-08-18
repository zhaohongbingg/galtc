package com.galtc2.content.machine;

import com.galtc2.Register.galtcBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * 矿石生成器的方块实体。
 * <p>
 * 每个 tick 消耗能量，到达阈值后生成矿石到上方方块/掉落物。
 */
public class OreGeneratorBlockEntity extends BlockEntity {

    /** 内部能量存储 */
    private final OreGeneratorEnergyStorage energy;
    private final LazyOptional<IEnergyStorage> energyOptional;

    /** 当前操作进度 (tick) */
    private int progress = 0;

    /** 随机数生成器 */
    private final Random random = new Random();

    public OreGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(galtcBlockEntity.ORE_GENERATOR.get(), pos, state);
        OreGeneratorConfig config = OreGeneratorConfig.get();
        this.energy = new OreGeneratorEnergyStorage(
                (int) Math.min(config.getEnergyCapacity(), Integer.MAX_VALUE),
                (int) Math.min(config.getEnergyPerTick() * 4, Integer.MAX_VALUE)
        );
        this.energyOptional = LazyOptional.of(() -> energy);
    }

    // ===== Tick 逻辑 =====

    public void tickServer() {
        if (level == null || level.isClientSide) return;
        OreGeneratorConfig config = OreGeneratorConfig.get();

        // 1. 从周围吸取能量（ForgeCapabilities.ENERGY）
        pullEnergyFromNeighbors(config);

        // 2. 检查是否有足够能量运行
        long perTick = config.getEnergyPerTick();
        if (energy.getEnergyStored() < perTick) {
            setLit(false);
            progress = 0;
            return;
        }

        // 3. 消耗能量 + 推进进度
        energy.extractEnergy((int) Math.min(perTick, Integer.MAX_VALUE), false);
        progress++;

        // 4. 更新 LIT 状态
        setLit(true);

        // 5. 进度满了，生成矿石
        if (progress >= config.getTicksPerOperation()) {
            progress = 0;

            // 按权重随机选一种矿石
            ItemStack result = config.rollOre(random);
            if (!result.isEmpty()) {
                // 进一步检查该矿石所需的能量是否充足
                long requiredEnergy = getRequiredEnergyFor(result, config);
                if (energy.getEnergyStored() >= requiredEnergy) {
                    energy.extractEnergy((int) Math.min(requiredEnergy, Integer.MAX_VALUE), false);
                    outputItem(result);
                }
            }
        }
    }

    /**
     * 从相邻方块的能量能力中抽取能量。
     */
    private void pullEnergyFromNeighbors(OreGeneratorConfig config) {
        if (level == null) return;
        int maxReceive = energy.getMaxReceive();
        if (maxReceive <= 0 || energy.getEnergyStored() >= energy.getMaxEnergyStored()) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null) continue;

            LazyOptional<IEnergyStorage> opt = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
            opt.ifPresent(source -> {
                if (source.canExtract()) {
                    int extracted = source.extractEnergy(maxReceive, false);
                    energy.receiveEnergy(extracted, false);
                }
            });
        }
    }

    /**
     * 获取指定矿石的所需能量。
     * 遍历配置，如果找到对应物品则返回其 energyPerOperation，否则使用默认值。
     */
    private long getRequiredEnergyFor(ItemStack stack, OreGeneratorConfig config) {
        for (OreGeneratorConfig.OreEntry entry : config.getOres()) {
            if (entry.getItem() == stack.getItem()) {
                return entry.getEnergyPerOperation();
            }
        }
        return 50_000; // 默认
    }

    /**
     * 将物品输出到上方方块或掉落物。
     */
    private void outputItem(ItemStack stack) {
        if (level == null || level.isClientSide) return;

        BlockPos above = worldPosition.above();
        // 尝试插入上方容器的第一个可用槽位
        BlockEntity aboveBe = level.getBlockEntity(above);
        if (aboveBe != null) {
            LazyOptional<net.minecraftforge.items.IItemHandler> itemOpt =
                    aboveBe.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN);
            itemOpt.ifPresent(handler -> {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack remaining = handler.insertItem(i, stack.copy(), false);
                    if (remaining.isEmpty()) {
                        stack.setCount(0);
                        break;
                    }
                    stack.setCount(remaining.getCount());
                }
            });
        }

        // 如果还有剩余，掉落在地上
        if (!stack.isEmpty() && level instanceof ServerLevel sl) {
            Containers.dropItemStack(sl, above.getX() + 0.5, above.getY() + 0.5, above.getZ() + 0.5, stack);
        }
    }

    // ===== 状态管理 =====

    private void setLit(boolean lit) {
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(OreGeneratorBlock.LIT) && state.getValue(OreGeneratorBlock.LIT) != lit) {
                level.setBlock(worldPosition, state.setValue(OreGeneratorBlock.LIT, lit), 3);
            }
        }
    }

    public int getProgress() { return progress; }
    public int getEnergyStored() { return energy.getEnergyStored(); }
    public int getMaxEnergy() { return energy.getMaxEnergyStored(); }
    public int getMaxReceive() { return energy.getMaxReceive(); }

    // ===== NBT 持久化 =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("energy")) {
            energy.setEnergy(tag.getInt("energy"));
        }
        if (tag.contains("progress")) {
            progress = tag.getInt("progress");
        }
    }

    // ===== Capability 暴露 =====

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
    }

    // ===== 内部 EnergyStorage =====

    private static class OreGeneratorEnergyStorage extends EnergyStorage {
        public OreGeneratorEnergyStorage(int capacity, int maxTransfer) {
            super(capacity, maxTransfer, maxTransfer);
        }

        public void setEnergy(int energy) {
            this.energy = energy;
        }

        public int getMaxReceive() {
            return maxReceive;
        }
    }
}
