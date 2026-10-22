package com.galtc2.compat.relicward.modifiers;

import cn.suiyi.relicward.combat.ArmorFracture;
import cn.suiyi.relicward.combat.CombatMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;

/**
 * 遗境守望(Relic Ward) 联动特性共用的小工具。
 * <p>
 * <b>本类会引用遗境守望 的类型，只能在确认遗境守望 已安装之后加载</b>
 * —— 也就是只能被 {@code RelicWardModifiers} 及其下面的强化类引用，
 * 而 {@code RelicWardModifiers} 又只在 {@link com.galtc2.compat.relicward.RelicWardCompat#init}
 * 判断通过后才会被触碰。
 * <p>
 * 护甲钩子（{@code MODIFY_HURT}）对「每件带该特性的护甲」各调一次，
 * 所以减伤这类效果要先确定"由哪一件护甲负责结算"，否则穿 4 件就会结算 4 次。
 * 这里按固定槽位顺序（脚→腿→胸→头）取第一件，和祸乱鬼魅 联动的 {@code BomdHelper} 同一套规则。
 */
public final class RelicWardHelper {

    /**
     * 遗境守望 自己的冷却槽名（{@link cn.suiyi.relicward.item.RelicEquipment} 的账本记在玩家身上、
     * 与手持物品无关，模组用它实现"同类技能冷却不会因更换物品而重置"）。
     * <p>
     * <b>注意</b>：回响震击<b>已经不用这套账本了</b> —— 按玩家记的冷却会牵连别的工具
     * （拿一把剑放完招，换另一把剑也在冷却里），所以它改成把冷却写进工具自己的持久数据、
     * 按工具各算各的（见 {@code EchoSlamModifier.KEY_COOLDOWN}）。
     * 这里留着 {@code "Maul"} / {@code "Wave"} / {@code "galtc2_puppet"} 三个槽名，
     * 是给钟波 / 巡庭铜傀 那两条暂时没启用的特性（以及以后想恢复"与模组本体共享冷却"的写法）备用。
     */
    public static final String WAVE_COOLDOWN_KEY = "Wave";
    public static final String MAUL_COOLDOWN_KEY = "Maul";
    /** 巡庭铜傀 是 galtc2 侧新增的招式，模组里没有对应槽，另起一个键。 */
    public static final String PUPPET_COOLDOWN_KEY = "galtc2_puppet";

    /** 参与结算顺序的盔甲槽位（脚→腿→胸→头） */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    /** 震击落点相对玩家的水平前移距离（模组取 1.8 格） */
    private static final double MAUL_REACH_AHEAD = 1.8;
    /** 震击落点从目标格往下最多探几格（模组取 5 格） */
    private static final int MAUL_GROUND_SCAN = 5;

    private RelicWardHelper() {
    }

    /**
     * 实体身上有没有遗境守望 的「裂甲」（护甲与护甲韧性 −30%）。
     * <p>
     * 就是钟波与震击打在人身上的那个破甲标记。当前的钟甲特性用不到它，
     * 留着给以后需要"按裂甲状态分支"的特性（例如模组里"失衡期间关闭钟甲"那套）。
     */
    public static boolean isFractured(LivingEntity entity) {
        return entity.hasEffect(ArmorFracture.EFFECT.get());
    }

    /** 穿戴了该强化的第一件护甲槽位；一件都没有时返回 null */
    @Nullable
    public static EquipmentSlot firstArmorSlot(EquipmentContext context, Modifier modifier) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(modifier) > 0) {
                return slot;
            }
        }
        return null;
    }

    /**
     * 回响钟锤「震击」的落点：照 {@code EchoMaulItem.groundPoint} 一比一复刻。
     * <ul>
     *   <li>取玩家位置沿视线水平方向前移 {@value #MAUL_REACH_AHEAD} 格的水平坐标；</li>
     *   <li>从该格上方开始最多往下探 {@value #MAUL_GROUND_SCAN} 格，
     *       第一个<b>碰撞箱非空</b>的方块就是落锤面；</li>
     *   <li>落点高度 = 那一格的方块底面 Y + 该方块碰撞箱在 Y 轴上的最高点（所以台阶、半砖也算得准）。</li>
     * </ul>
     * 前方是悬空（下面 {@value #MAUL_GROUND_SCAN} 格内全空）时返回 {@code null}，
     * 调用方据此提示"前方没有可捶击的地面"并跳过本次震击 —— 与回响钟锤的表现一致。
     */
    @Nullable
    public static Vec3 maulGroundPoint(Level level, Player player) {
        Vec3 ahead = player.position().add(CombatMath.forward(player.getYRot()).scale(MAUL_REACH_AHEAD));
        BlockPos start = BlockPos.containing(ahead).above();
        for (int i = 0; i < MAUL_GROUND_SCAN; i++) {
            BlockPos pos = start.below(i);
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return new Vec3(ahead.x, pos.getY() + shape.max(Direction.Axis.Y), ahead.z);
            }
        }
        return null;
    }
}
