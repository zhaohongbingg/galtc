package com.galtc2.compat.relicward.modifiers;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.combat.CombatMath;
import cn.suiyi.relicward.entity.CourtPuppet;
import cn.suiyi.relicward.item.RelicEquipment;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 巡庭铜傀 (court_puppet) —— 遗境守望 联动强化（占用能力槽）。
 * <p>
 * 右键召来一只自己的 {@link CourtPuppet}（{@link RelicWard#COURT_PUPPET}），
 * 认主流程完全照模组的「契律印」那条路走（见 {@code CourtPuppet#mobInteract} 里收服那一段）：
 * <ol>
 *   <li>{@code puppet.tame(player)} —— 一步把主人写上并置为已驯服；</li>
 *   <li>{@code setPersistenceRequired()} —— 不参与自然清除，跟着主人跨维度/重登；</li>
 *   <li>生命上限与当前生命都设成 {@value #PUPPET_HEALTH} 点 —— 与契律印收服后的伙伴同数值；</li>
 *   <li>照模组播 {@code broadcastEntityEvent(puppet, (byte) 7)}（收服时的爱心粒子）+ 钟鸣音效，
 *       并弹模组自己的 {@code message.relicward.bound}（"契约已成：空手右键切换跟随/待命…"）。</li>
 * </ol>
 * 之后铜傀的行动逻辑（跟随、护主、待命、攻击动画）全是模组自己的 AI 目标，
 * 这边一行都不重写；空手右键它一样能切跟随/待命，用铜傀修补膏也能修 —— 与野生收服的伙伴没有任何区别。
 * <p>
 * <b>与「契律印」的界限</b>：玩家用契律印收服的伙伴不会被本特性动到。
 * 本特性召唤出来的每一只都会打上 {@value #KEY_SUMMONED} 标记，
 * 再召唤时<b>只回收带标记且主人是自己</b>的那一只（避免无限堆积），
 * 顺手把"上一只还活着就又召一只"变成"换一只"。
 * <p>
 * <b>冷却</b>：{@value #COOLDOWN} tick（3 分钟），走遗境守望 的冷却账本
 * （{@link RelicEquipment} 的 {@link RelicWardHelper#PUPPET_COOLDOWN_KEY} 槽）。
 * 铜傀本身不会自动复活（模组设定），冷却就是"再召一只"的代价。
 */
public class PuppetSummonModifier extends Modifier implements GeneralInteractionModifierHook {

    /** 冷却（tick）：3 分钟 */
    private static final int COOLDOWN = 3 * 60 * 20;
    /** 召唤距离：玩家正前方几格（模组生成野生铜傀也是贴着一个点放） */
    private static final double SPAWN_DISTANCE = 2.0;
    /** 伙伴生命上限：与契律印收服后的铜傀一致 */
    private static final float PUPPET_HEALTH = 32.0f;
    /** 回收旧伙伴的搜索半径（格） */
    private static final double RECYCLE_RADIUS = 64.0;
    /** 标记：本特性召出来的铜傀（用来和契律印收服的伙伴区分开） */
    private static final String KEY_SUMMONED = "galtc2_relicward_puppet";

    public PuppetSummonModifier() {
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
        if (player.level() instanceof ServerLevel level && !summon(level, player)) {
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    /** 招一只铜傀；冷却中或生成失败返回 false */
    private static boolean summon(ServerLevel level, Player player) {
        if (RelicEquipment.cooldown(player, RelicWardHelper.PUPPET_COOLDOWN_KEY) > 0) {
            return false;
        }
        recycle(level, player);

        CourtPuppet puppet = RelicWard.COURT_PUPPET.get().create(level);
        if (puppet == null) {
            return false;
        }
        Vec3 pos = player.position().add(CombatMath.forward(player.getYRot()).scale(SPAWN_DISTANCE));
        puppet.moveTo(pos.x, player.getY(), pos.z, player.getYRot(), 0.0f);
        // 契律印那套认主流程
        puppet.tame(player);
        puppet.setPersistenceRequired();
        puppet.setTarget(null);
        puppet.getAttribute(Attributes.MAX_HEALTH).setBaseValue(PUPPET_HEALTH);
        puppet.setHealth(PUPPET_HEALTH);
        puppet.getPersistentData().putBoolean(KEY_SUMMONED, true);
        if (!level.addFreshEntity(puppet)) {
            return false;
        }

        level.broadcastEntityEvent(puppet, (byte) 7);
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0f, 1.4f);
        player.displayClientMessage(Component.translatable("message.relicward.bound"), true);
        RelicEquipment.setCooldown(player, RelicWardHelper.PUPPET_COOLDOWN_KEY, COOLDOWN);
        return true;
    }

    /** 回收上一只由本特性召出的铜傀（带标记 + 主人是自己）；契律印收服的伙伴不碰 */
    private static void recycle(ServerLevel level, Player player) {
        for (CourtPuppet old : level.getEntitiesOfClass(CourtPuppet.class,
                player.getBoundingBox().inflate(RECYCLE_RADIUS))) {
            if (old.getPersistentData().getBoolean(KEY_SUMMONED)
                    && player.getUUID().equals(old.getOwnerUUID())) {
                old.discard();
            }
        }
    }
}
