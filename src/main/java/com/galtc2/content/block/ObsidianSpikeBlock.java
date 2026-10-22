package com.galtc2.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 黑曜石地刺。
 * <p>
 * 由祸乱鬼魅 联动强化「宝石少女」在目标脚下生成：站上去会持续受伤，
 * 一段短时间后自行消失（照原版「唤魔者尖牙」那种"来一下就走"的思路，不长期留在地形上）。
 * <p>
 * 生命周期用计划刻（{@link #tick}）实现，所以方块本身<b>不需要 blockstate 属性</b>，
 * 模型就是一个静态模型。
 */
public class ObsidianSpikeBlock extends Block {

    /** 存在时长（tick），1 秒 = 20 */
    private static final int LIFETIME_TICKS = 60;
    /** 每次触发对站在上面的生物造成的伤害 */
    private static final float DAMAGE = 4.0f;
    /** 同一生物两次受伤之间的最短间隔（tick），避免每一刻都扣血 */
    private static final int DAMAGE_COOLDOWN_TICKS = 10;

    public ObsidianSpikeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            // 每次放置都重置一次倒计时
            level.scheduleTick(pos, this, LIFETIME_TICKS);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        // 用实体自身的无敌帧天然错开重复伤害，不必自己记时间戳
        if (living.invulnerableTime > 0) {
            return;
        }
        living.hurt(level.damageSources().cactus(), DAMAGE);
        living.invulnerableTime = DAMAGE_COOLDOWN_TICKS;
    }
}
