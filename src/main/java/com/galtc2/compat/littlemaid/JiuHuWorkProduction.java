package com.galtc2.compat.littlemaid;

import com.galtc2.Register.galtcItem;
import com.galtc2.compat.littlemaid.modifiers.JiuHuHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 酒狐的「工作效率」：酒狐干活会攒下 {@code galtc2:jiuhu_ingot}（酒狐锭），
 * <b>放进她自己的背包</b>（{@link EntityMaid#getMaidInv()}，也就是女仆 GUI 里的物品栏）；
 * 背包塞满了就掉在她脚边，不会凭空消失。
 * <p>
 * <b>产出节奏（按好感度分级，固定间隔、不再随机）：</b>
 * <table>
 *   <tr><th>好感度</th><th>间隔</th></tr>
 *   <tr><td>0 ~ 63（0 级）</td><td>5 分钟 1 个</td></tr>
 *   <tr><td>64 ~ 127（1 级）</td><td>3 分钟 1 个</td></tr>
 *   <tr><td>128 ~ 191（2 级）</td><td>2 分钟 1 个</td></tr>
 *   <tr><td>192 以上（3 级）</td><td>1 分钟 1 个</td></tr>
 * </table>
 * 只有「任务不是待机、且没坐下」的干活状态才会计时；累计计时存在女仆自己的
 * persistentData 里，所以重进存档 / 区块重载都不会丢进度或重复结算。
 * <p>
 * <b>疲劳</b>：连续工作满 {@link #WORK_LIMIT_TICKS} 刻（= 游戏内 1 天 / 现实 20 分钟）后她就累趴了，
 * 产出<b>停止</b>；必须<b>连续休息满 {@link #REST_RESET_TICKS} 刻（也是 1 天）</b>才会清空疲劳、
 * 重新开始攒。半途去干活会打断休息进度。累趴 / 休息好各给主人一条提示。
 * <p>
 * 车万女仆 的任务枚举见 {@code entity/task/Task*.class}：农场 / 养蜂 / 剪羊毛 / 钓鱼 /
 * 挤奶 / 可可 / 甘蔗 / 西瓜 / 除雪 / 点火把 …… 任一非待机任务都算「在干活」。
 * 纯服务端逻辑，客户端不跑；只认「被玩家驯服的女仆」，野生的不产。
 */
public final class JiuHuWorkProduction {

    /** 干活产出间隔（tick）：0/1/2/3 级好感度 = 5/3/2/1 分钟 */
    private static final int[] PRODUCE_INTERVAL_TICKS = {20 * 300, 20 * 180, 20 * 120, 20 * 60};

    /** 好感度每一级的点数（64 / 128 / 192 为 1/2/3 级门槛） */
    private static final int FAVORABILITY_PER_LEVEL = 64;

    /** 连续工作多久后累趴：24000 刻 = 游戏内 1 天 */
    public static final int WORK_LIMIT_TICKS = 24000;

    /** 休息多久清空疲劳：24000 刻 = 游戏内 1 天 */
    public static final int REST_RESET_TICKS = 24000;

    /* 女仆 persistentData 里的键 */

    /** 已连续工作了多少刻 */
    private static final String WORK_TICKS = "galtc2:jiuhu_work_ticks";

    /** 已连续休息了多少刻 */
    private static final String REST_TICKS = "galtc2:jiuhu_rest_ticks";

    /** 下一次产出的游戏刻 */
    private static final String NEXT_PRODUCE = "galtc2:jiuhu_next_produce";

    private static boolean registered = false;

    private JiuHuWorkProduction() {
    }

    /** 由 {@link LittleMaidCompat#init} 调用（只在装了车万女仆 时执行） */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(JiuHuWorkProduction::onLivingTick);
    }

    /** 当前好感度对应的产出间隔（tick） */
    public static int produceIntervalOf(EntityMaid maid) {
        int level = Math.min(PRODUCE_INTERVAL_TICKS.length - 1,
                JiuHuHelper.favorability(maid) / FAVORABILITY_PER_LEVEL);
        return PRODUCE_INTERVAL_TICKS[level];
    }

    /** 是不是已经干满一天、累到停产了 */
    public static boolean isFatigued(EntityMaid maid) {
        return maid.getPersistentData().getInt(WORK_TICKS) >= WORK_LIMIT_TICKS;
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid)) {
            return;
        }
        Level level = maid.level();
        if (level.isClientSide() || !JiuHuHelper.isJiuHu(maid)) {
            return;
        }
        boolean working = isWorking(maid);
        // 疲劳计时按刻累计，所以每刻都要走一遍（只是读写两个整数）
        updateFatigue(maid, working);

        if (!working || isFatigued(maid)) {
            return;
        }
        long gameTime = level.getGameTime();
        CompoundTag data = maid.getPersistentData();
        long next = data.getLong(NEXT_PRODUCE);
        if (next <= 0L) {
            // 第一次干活：从现在开始计时，不补发
            data.putLong(NEXT_PRODUCE, gameTime + produceIntervalOf(maid));
            return;
        }
        if (gameTime < next) {
            return;
        }
        give(maid, 1);
        data.putLong(NEXT_PRODUCE, gameTime + produceIntervalOf(maid));
    }

    /* ---------------------------------------------------------------- 疲劳 */

    /**
     * 工作累计满 {@link #WORK_LIMIT_TICKS} 刻进入疲劳；疲劳后连续休息满
     * {@link #REST_RESET_TICKS} 刻才清零。中途去干活会把休息进度打断。
     */
    private static void updateFatigue(EntityMaid maid, boolean working) {
        CompoundTag data = maid.getPersistentData();
        int workTicks = data.getInt(WORK_TICKS);
        int restTicks = data.getInt(REST_TICKS);

        if (working) {
            if (restTicks != 0) {
                data.putInt(REST_TICKS, 0);
            }
            if (workTicks < WORK_LIMIT_TICKS) {
                workTicks++;
                data.putInt(WORK_TICKS, workTicks);
                if (workTicks >= WORK_LIMIT_TICKS) {
                    notifyOwner(maid, "message.galtc2.jiuhu_tired");
                }
            }
            return;
        }

        // 没在干活：只有已经工作过才需要算休息，避免挂机一天后又多发一次提示
        if (workTicks <= 0) {
            return;
        }
        restTicks++;
        if (restTicks >= REST_RESET_TICKS) {
            data.putInt(WORK_TICKS, 0);
            data.putInt(REST_TICKS, 0);
            data.remove(NEXT_PRODUCE);
            if (workTicks >= WORK_LIMIT_TICKS) {
                notifyOwner(maid, "message.galtc2.jiuhu_rested");
            }
            return;
        }
        data.putInt(REST_TICKS, restTicks);
    }

    private static void notifyOwner(EntityMaid maid, String key) {
        if (maid.getOwner() instanceof Player owner) {
            owner.displayClientMessage(Component.translatable(key), false);
        }
    }

    /* ---------------------------------------------------------------- 产出 */

    /**
     * 放进女仆自己的背包；背包满了就把剩下的掉在她脚边。
     * 注意 {@code insertItem} 会返回「塞不进去的部分」，所以要逐个槽位接着塞。
     */
    private static void give(EntityMaid maid, int count) {
        ItemStackHandler backpack = maid.getMaidInv();
        for (int i = 0; i < count; i++) {
            ItemStack remaining = new ItemStack(galtcItem.jiuhu_ingot.get());
            for (int slot = 0; slot < backpack.getSlots() && !remaining.isEmpty(); slot++) {
                remaining = backpack.insertItem(slot, remaining, false);
            }
            if (!remaining.isEmpty()) {
                maid.spawnAtLocation(remaining);
            }
        }
    }

    /** 任务不是待机、也没坐着 = 在干活 */
    private static boolean isWorking(EntityMaid maid) {
        if (maid.isMaidInSittingPose()) {
            return false;
        }
        return maid.getTask() != null && maid.getTask() != TaskManager.getIdleTask();
    }
}
