package com.galtc2.event;

import com.galtc2.Galtc2;
import com.galtc2.modifiers.TianjinZuiModifier;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;

/**
 * 「天津罪·其二」的实现：开着领域时，范围内的生物无法生成。
 * <p>
 * 拦生成需要<b>两个</b>钩子，因为原版的"生成"不止一条路：
 * <ul>
 *   <li>{@link MobSpawnEvent.FinalizeSpawn} —— <b>常规路径</b>：自然刷怪、刷怪笼、结构、繁殖、袭击。
 *       这些都会调 {@code Mob.finalizeSpawn(...)}，事件里还带着 {@link MobSpawnType}，
 *       所以能精确地把"玩家主动召唤"（刷怪蛋 / 指令）放行；</li>
 *   <li>{@link EntityJoinLevelEvent} —— <b>兜底路径</b>：史莱姆分裂、挖虫蚀石钻出蠹虫、
 *       僵尸增援、村民转化…… 这些是直接 {@code level.addFreshEntity(...)} 进来的，
 *       <b>压根不经过 {@code finalizeSpawn}</b>，只监听前者会全部漏掉。</li>
 * </ul>
 * {@code EntityJoinLevelEvent} 靠 {@code loadedFromDisk()} 把"读存档时重新加载的已有实体"排除掉
 * —— 少了这一步，玩家一走近区块就会把范围里原本就存在的怪全部抹掉。
 * <p>
 * 判断方式不是维护一份「领域坐标表」，而是直接看<b>附近有没有玩家正带着该特性</b> ——
 * 刷怪频率本来就不高，遍历附近玩家比维护状态简单，也不会出现"玩家走了领域没清掉"这类不同步。
 * 半径与禁飞共用 {@link TianjinZuiModifier#radiusFor(float)}，两个特性的作用范围始终一致。
 */
@Mod.EventBusSubscriber(modid = Galtc2.MODID)
public final class TianjinZuiSpawnBanEvents {

    /** 触发领域的那条特性 */
    private static final ModifierId TIANJIN_ZUI_NO_SPAWN =
            new ModifierId(Galtc2.MODID, "tianjin_zui_no_spawn");

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    /**
     * 「已明确放行」的标记，写在实体的持久数据上。
     * <p>
     * {@code FinalizeSpawn} 里放行了刷怪蛋 / 指令之后打上它，好让紧接着触发的
     * {@code EntityJoinLevelEvent} 别再把同一只生物拦一次；那边检查时会顺手把它清掉。
     * <p>
     * <b>别改回存时间戳</b>：早先这里存的是 {@code tickCount}，那是错的 ——
     * 刚创建出来的实体 {@code tickCount} 就是 0，而 {@code getInt} 读不到键时也返回 0，
     * 两者相等，于是<b>所有新实体都被误判成"已放行"</b>，兜底这层形同虚设
     * （史莱姆分裂、虫蚀石钻蠹虫一律穿过去，而常规刷怪因为走的是上面那条路，看起来却是好的）。
     */
    private static final String KEY_ALLOWED = "galtc2_spawn_allowed";

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 日志限流用；游戏时间从 0 开始，给个足够小的初值即可 */
    private static long lastLogTick = -100L;

    private TianjinZuiSpawnBanEvents() {
    }

    // ------------------------------------------------------------------ 常规路径

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        MobSpawnType type = event.getSpawnType();
        // 刷怪蛋与指令是玩家主动行为，不该被领域拦下来。打个标记，下面那条兜底别再拦一次。
        if (type == MobSpawnType.SPAWN_EGG || type == MobSpawnType.COMMAND) {
            event.getEntity().getPersistentData().putBoolean(KEY_ALLOWED, true);
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (insideField(level, event.getX(), event.getY(), event.getZ())) {
            event.setSpawnCancelled(true);
            logBlocked(level, event.getEntity());
        }
    }

    // ------------------------------------------------------------------ 兜底路径

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // 从存档里读出来的旧实体绝对不能动，否则玩家一靠近就把既有的生物清空了
        if (event.loadedFromDisk()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        // FinalizeSpawn 那边已经明确放行过的（刷怪蛋 / 指令）—— 标记用完即清，不留残留
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean(KEY_ALLOWED)) {
            data.remove(KEY_ALLOWED);
            return;
        }
        if (insideField(level, mob.getX(), mob.getY(), mob.getZ())) {
            event.setCanceled(true);
            logBlocked(level, mob);
        }
    }

    // ------------------------------------------------------------------ 共用判定

    /** 这个坐标是否落在某个「天津罪·其二」领域里 */
    private static boolean insideField(ServerLevel level, double x, double y, double z) {
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive()) {
                continue;
            }
            int auraLevel = auraLevel(player);
            if (auraLevel <= 0) {
                continue;
            }
            double radius = TianjinZuiModifier.radiusFor(auraLevel);
            if (player.distanceToSqr(x, y, z) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    /**
     * 玩家身上「天津罪·其二」的最高等级（主手 / 副手 / 四件护甲里取最大）。
     * <p>
     * 和禁飞那边一致：只认拿在手上或穿在身上的，躺在背包里不算。
     *
     * @return 没有则返回 0
     */
    private static int auraLevel(Player player) {
        int best = ModifierUtil.getModifierLevel(player.getMainHandItem(), TIANJIN_ZUI_NO_SPAWN);
        best = Math.max(best, ModifierUtil.getModifierLevel(player.getOffhandItem(), TIANJIN_ZUI_NO_SPAWN));
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            best = Math.max(best, ModifierUtil.getModifierLevel(player.getItemBySlot(slot), TIANJIN_ZUI_NO_SPAWN));
        }
        return best;
    }

    /**
     * 打一条拦截日志，用于进游戏里核对特性到底有没有生效。
     * <p>
     * 站在黑暗处刷怪会非常密集，所以限流成每秒最多一条。
     * 调试完把这一行删掉即可，不影响功能。
     */
    private static void logBlocked(ServerLevel level, Mob mob) {
        long now = level.getGameTime();
        if (now - lastLogTick < 20L) {
            return;
        }
        lastLogTick = now;
        LOGGER.info("[天津罪·其二] 拦下生成：{} @ {}",
                mob.getType().getDescription().getString(), mob.blockPosition());
    }
}
