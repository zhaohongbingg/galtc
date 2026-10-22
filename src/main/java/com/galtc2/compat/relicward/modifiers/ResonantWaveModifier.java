package com.galtc2.compat.relicward.modifiers;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.entity.ResonantWave;
import cn.suiyi.relicward.item.RelicEquipment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 钟波 (resonant_wave) —— 遗境守望 联动强化（占用能力槽）。
 * <p>
 * 站在地面上右键，释放一圈扩散钟波 —— 与震律鸣钟是同<b>一个</b>东西：
 * 直接生成模组的 {@link ResonantWave} 实体（{@link RelicWard#RESONANT_WAVE}），
 * 伤害、半径、裂甲、扩散动画、客户端渲染全部由模组那套自己跑，这边不复制任何逻辑。
 * 波环本身的数据（读源码/字节码核对过）：
 * <ul>
 *   <li>寿命 36 tick，从玩家脚下起扩到半径 20 格；</li>
 *   <li>每个目标吃 <b>8 点</b>伤害（{@code playerAttack} 伤害源）+ {@code ArmorFracture.apply}（8 秒裂甲）；</li>
 *   <li>会跳过波环画出的平面之外、以及主人自己与其友军。</li>
 * </ul>
 * <b>冷却</b>：照 {@link cn.suiyi.relicward.item.ResonantBellItem}（8 秒 / {@value #COOLDOWN} tick），
 * 并写在遗境守望 自己的冷却账本 {@link RelicEquipment} 的 {@code "Wave"} 槽上
 * —— 也就是<b>和震律鸣钟共用同一个冷却</b>（模组的账本记在玩家身上、与手持物品无关，
 * 本来就是给"换武器也逃不掉冷却"准备的，见 {@link RelicWardHelper#WAVE_COOLDOWN_KEY}）。
 * <p>
 * 起手条件与震律鸣钟一致：<b>必须站在地面上</b>（{@code player.onGround()}），
 * 空中、水里按不出来。音效也用模组那一声 {@link SoundEvents#BELL_RESONATE}（1.2 / 0.8）。
 * <p>
 * 本特性是固定效果（与祸乱鬼魅 联动的 {@code void_thorn} / {@code blazing_eye} 一样不做等级成长）：
 * 波环的伤害写在实体内部，不随强化等级变化。
 */
public class ResonantWaveModifier extends Modifier implements GeneralInteractionModifierHook {

    /** 冷却（tick）：照震律鸣钟的 8 秒 */
    private static final int COOLDOWN = 160;

    public ResonantWaveModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player,
                                       InteractionHand hand, InteractionSource source) {
        if (source != InteractionSource.RIGHT_CLICK || tool.isBroken()) {
            return InteractionResult.PASS;
        }
        // 照震律鸣钟：得站在地上才敲得响
        if (!player.onGround()) {
            return InteractionResult.PASS;
        }
        // 客户端不做判定（模组的账本在服务端才算得准），照震律鸣钟的写法：
        // 服务端放得出就 SUCCESS，放不出（冷却中 / 实体生成失败）就 PASS。
        if (player.level() instanceof ServerLevel level && !cast(level, player, hand)) {
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    /** 放一圈钟波；冷却中或生成失败返回 false */
    private static boolean cast(ServerLevel level, Player player, InteractionHand hand) {
        if (RelicEquipment.cooldown(player, RelicWardHelper.WAVE_COOLDOWN_KEY) > 0) {
            return false;
        }
        ResonantWave wave = RelicWard.RESONANT_WAVE.get().create(level);
        if (wave == null) {
            return false;
        }
        // initialize 一步到位：记下主人、出生时间（BORN），并把自己挪到玩家脚下
        wave.initialize(player);
        if (!level.addFreshEntity(wave)) {
            return false;
        }
        RelicEquipment.setCooldown(player, RelicWardHelper.WAVE_COOLDOWN_KEY, COOLDOWN);
        // 物品栏上的灰度冷却照模组也刷一下（模组给震律鸣钟刷的是它自己那件物品）
        Item used = player.getItemInHand(hand).getItem();
        player.getCooldowns().addCooldown(used, COOLDOWN);
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.2f, 0.8f);
        return true;
    }
}
