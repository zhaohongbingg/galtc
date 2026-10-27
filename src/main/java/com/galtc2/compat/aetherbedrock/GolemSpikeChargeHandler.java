package com.galtc2.compat.aetherbedrock;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 晶刺冲撞 (golem_spike_charge) 的触发逻辑 —— <b>方案 A：语义触发（疾跑跳落地）</b>。
 * <p>
 * 不再依赖「速度数值」。判定分为三步，全部用<b>位置差</b>而不是 {@code getDeltaMovement()}
 * （服务端玩家的速度向量不可靠）：
 * <ol>
 *   <li><b>起跳</b>：上一 tick 还在地面、这一 tick 离地，同时 {@code player.isSprinting()}
 *       且这一 tick 向上位移 &gt; 0 → 记下「这是一次疾跑跳」；</li>
 *   <li><b>滞空</b>：累计滞空 tick，并记录最大向上位移（用来排除「走下悬崖」而不是跳起来）；</li>
 *   <li><b>落地</b>：从空中回到地面，若这次是疾跑跳（滞空 ≥3 tick 且确实向上过）、
 *       冷却已好 → 沿水平移动方向（没有位移就用视线方向）在身后铺一串晶刺（7 根）。</li>
 * </ol>
 * 冷却 10 秒，记在玩家自己的持久数据里；骑乘/载具/飞行/水中/旁观时整个状态清零。
 */
public final class GolemSpikeChargeHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 强化 id，用于检查四件护甲上有没有这条强化 */
    private static final ModifierId MODIFIER_ID = new ModifierId("galtc2", "golem_spike_charge");

    private static final String KEY_COOLDOWN = "galtc2_golem_spike_charge_cd";
    private static final String KEY_LAST_X = "galtc2_golem_spike_last_x";
    private static final String KEY_LAST_Y = "galtc2_golem_spike_last_y";
    private static final String KEY_LAST_Z = "galtc2_golem_spike_last_z";
    private static final String KEY_LAST_SET = "galtc2_golem_spike_last_set";
    private static final String KEY_WAS_ON_GROUND = "galtc2_golem_spike_was_ground";
    private static final String KEY_SPRINT_JUMP = "galtc2_golem_spike_sprint_jump";
    private static final String KEY_AIR_TICKS = "galtc2_golem_spike_air_ticks";
    private static final String KEY_MAX_UP = "galtc2_golem_spike_max_up";

    /** 冷却：10 秒 */
    private static final int COOLDOWN_TICKS = 200;
    /** 判定为「跳起来」而非「走下去」的最小向上位移（格/tick） */
    private static final double MIN_UP_MOVE = 0.05D;
    /** 非疾跑时也能触发的最低水平速度：5.8 格/秒（速度 II 走路约 6.0，速度 I 走路约 5.2） */
    private static final double FAST_JUMP_SPEED = 5.8D / 20.0D;
    /** 判定为「跳过」的最小滞空 tick */
    private static final int MIN_AIR_TICKS = 3;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** 每个玩家只在第一次 tick 打印一次护甲检测结果，方便确认强化到底装没装上 */
    private static final Map<UUID, Boolean> LOGGED = new ConcurrentHashMap<>();

    private GolemSpikeChargeHandler() {
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide()) {
            return;
        }
        CompoundTag data = player.getPersistentData();

        boolean hasArmor = hasArmorModifier(player);
        if (LOGGED.putIfAbsent(player.getUUID(), Boolean.TRUE) == null) {
            LOGGER.info("[galtc2] 晶刺冲撞：handler 已生效，护甲强化检测 = {}", hasArmor);
        }

        // 冷却每 tick 递减（脱了护甲也继续走）
        int cooldown = data.getInt(KEY_COOLDOWN);
        if (cooldown > 0) {
            data.putInt(KEY_COOLDOWN, cooldown - 1);
        }
        boolean ready = cooldown <= 0;

        // 位移（位置差：服务端玩家的 deltaMovement 不可靠）
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        boolean lastSet = data.getBoolean(KEY_LAST_SET);
        double dx = lastSet ? x - data.getDouble(KEY_LAST_X) : 0.0D;
        double dy = lastSet ? y - data.getDouble(KEY_LAST_Y) : 0.0D;
        double dz = lastSet ? z - data.getDouble(KEY_LAST_Z) : 0.0D;
        data.putDouble(KEY_LAST_X, x);
        data.putDouble(KEY_LAST_Y, y);
        data.putDouble(KEY_LAST_Z, z);
        data.putBoolean(KEY_LAST_SET, true);

        boolean onGround = player.onGround();
        boolean wasOnGround = data.getBoolean(KEY_WAS_ON_GROUND);
        data.putBoolean(KEY_WAS_ON_GROUND, onGround);

        // 状态无效的场景：整段清零，别在骑龙/飞行/水里误判
        boolean invalid = player.isSpectator() || player.getAbilities().flying || player.isFallFlying()
                || player.isPassenger() || player.getVehicle() != null
                || player.isInWaterOrBubble() || player.isInLava();
        if (invalid) {
            clearJumpState(data);
            return;
        }

        // 起跳：上一 tick 在地面 → 这一 tick 离地，且确实向上。
        // 只要「疾跑中」或「水平速度够快（速度药水等）」都算，光跳跃也能触发。
        if (wasOnGround && !onGround && (dy > 0.0D || player.getDeltaMovement().y > 0.0D)) {
            double horizontalSpeed = Math.sqrt(dx * dx + dz * dz);
            boolean sprintJump = player.isSprinting();
            boolean fastJump = horizontalSpeed >= FAST_JUMP_SPEED;
            if (sprintJump || fastJump) {
                data.putBoolean(KEY_SPRINT_JUMP, true);
                data.putInt(KEY_AIR_TICKS, 0);
                data.putDouble(KEY_MAX_UP, dy);
                LOGGER.debug("[galtc2] 晶刺冲撞：检测到跳跃（疾跑={}，水平速度={} 格/秒）",
                        sprintJump, String.format("%.2f", horizontalSpeed * 20.0D));
            }
        }

        if (!data.getBoolean(KEY_SPRINT_JUMP)) {
            return;
        }

        if (!onGround) {
            // 滞空：累计 tick + 记录最大向上位移
            data.putInt(KEY_AIR_TICKS, data.getInt(KEY_AIR_TICKS) + 1);
            if (dy > data.getDouble(KEY_MAX_UP)) {
                data.putDouble(KEY_MAX_UP, dy);
            }
            return;
        }

        // 落地：判断是不是「真的跳起来过」
        int airTicks = data.getInt(KEY_AIR_TICKS);
        double maxUp = data.getDouble(KEY_MAX_UP);
        clearJumpState(data);
        if (!ready || airTicks < MIN_AIR_TICKS || maxUp < MIN_UP_MOVE) {
            return;
        }
        if (!hasArmor) {
            return;
        }

        // 触发：方向用水平位移，没有就用视线方向
        Vec3 direction = new Vec3(dx, 0.0D, dz);
        if (direction.lengthSqr() < 1.0E-6D) {
            Vec3 look = player.getLookAngle();
            direction = new Vec3(look.x, 0.0D, look.z);
        }
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        data.putInt(KEY_COOLDOWN, COOLDOWN_TICKS);
        if (player.level() instanceof ServerLevel serverLevel) {
            AetherBedrockEffects.golemSpikeCharge(serverLevel, player, direction);
            LOGGER.info("[galtc2] 晶刺冲撞触发：疾跑跳落地（滞空 {} tick，最大上移 {}）", airTicks, maxUp);
        }
    }

    private static void clearJumpState(CompoundTag data) {
        data.putBoolean(KEY_SPRINT_JUMP, false);
        data.putInt(KEY_AIR_TICKS, 0);
        data.putDouble(KEY_MAX_UP, 0.0D);
    }

    /** 四件护甲里任意一件带这条强化即可 */
    private static boolean hasArmorModifier(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ModifiableArmorItem)) {
                continue;
            }
            if (ToolStack.from(stack).getModifiers().getLevel(MODIFIER_ID) > 0) {
                return true;
            }
        }
        return false;
    }
}
