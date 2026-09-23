package com.galtc2.compat.jerotes;

import com.galtc2.Galtc2;
import com.jerotes.jerotes.item.FlawlessMagicScroll;
import com.jerotes.jerotes.spell.MagicSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import javax.annotation.Nullable;

/**
 * 卷轴·法术刻印 (scroll_cast) —— J村魔法联动特性。
 * <ul>
 *   <li>手持本工具 + 副手"无暇魔法卷轴"(已带法术) + 潜行：把卷轴法术抄录进工具并消耗卷轴</li>
 *   <li>近战命中时施放近战型/自施型法术；右键生物时施放其它非近战法术</li>
 * </ul>
 * 实际施法委托给现成的 {@link SpellCastHelper}（借用自 jvt），法术类型不写死。
 */
public class ScrollCastModifier extends Modifier
        implements InventoryTickModifierHook, MeleeHitModifierHook, EntityInteractionModifierHook {

    /** 工具持久数据中存放法术 ID/等级的键 */
    private static final ResourceLocation KEY_SPELL = Galtc2.loc("jerotes_scroll_spell");
    private static final ResourceLocation KEY_SPELL_LEVEL = Galtc2.loc("jerotes_scroll_spell_level");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK, ModifierHooks.MELEE_HIT, ModifierHooks.ENTITY_INTERACT);
    }

    /* ---------- 潜行绑定：把副手卷轴的法术抄到工具上 ---------- */

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder,
                                int slot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide()) return;
        if (!isCorrectSlot) return;
        if (!(holder instanceof Player player)) return;
        if (!player.isShiftKeyDown()) return;

        ModDataNBT data = tool.getPersistentData();
        if (data.contains(KEY_SPELL)) return; // 已绑定过

        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof FlawlessMagicScroll)) return;

        String spellId = getSpellId(offhand);
        if (spellId == null || spellId.isEmpty()) return;
        if (SpellCastHelper.makeSpell(spellId, 1, player, player) == null) return;

        int spellLevel = offhand.getOrCreateTag().getInt(FlawlessMagicScroll.SPELL_LEVEL);
        if (spellLevel <= 0) spellLevel = 1;

        data.putString(KEY_SPELL, spellId);
        data.putInt(KEY_SPELL_LEVEL, spellLevel);
        offhand.shrink(1);

        player.displayClientMessage(
                Component.literal("[galtc2·卷轴刻印] 已抄录法术: " + spellId + " (等级 " + spellLevel + ")"), false);
    }

    /* ---------- 近战命中触发 ---------- */

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        if (attacker == null || attacker.level().isClientSide()) return;
        LivingEntity target = context.getLivingTarget();
        if (target == null) return;
        tryCast(tool, attacker, target, true);
    }

    /* ---------- 右键生物触发 ---------- */

    @Override
    public InteractionResult beforeEntityUse(IToolStackView tool, ModifierEntry modifier, Player player, Entity target,
                                             InteractionHand hand, InteractionSource source) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (target == null || player.level().isClientSide()) return InteractionResult.PASS;
        boolean cast = tryCast(tool, player, target, false);
        return player.level().isClientSide() ? InteractionResult.PASS
                : (cast ? InteractionResult.SUCCESS : InteractionResult.PASS);
    }

    /* ---------- 核心施法：委托给现成的 SpellCastHelper ---------- */

    private boolean tryCast(IToolStackView tool, LivingEntity caster, @Nullable Entity explicitTarget, boolean meleeHit) {
        ModDataNBT data = tool.getPersistentData();
        if (!data.contains(KEY_SPELL)) return false;
        String spellId = data.get(KEY_SPELL, CompoundTag::getString);
        if (spellId == null || spellId.isEmpty()) return false;
        int spellLevel = Math.max(1, data.get(KEY_SPELL_LEVEL, CompoundTag::getInt));

        if (meleeHit) {
            // 近战位只放近战/自施型，由 castMelee 内部判定
            if (!(explicitTarget instanceof LivingEntity target)) return false;
            return SpellCastHelper.castMelee(spellId, spellLevel, caster, target);
        }

        // 右键位不放近战型
        MagicSpell probe = SpellCastHelper.makeSpell(spellId, spellLevel, caster, caster);
        if (probe == null || probe.isMelee()) return false;
        return SpellCastHelper.cast(spellId, spellLevel, caster, explicitTarget);
    }

    /* ---------- 读卷轴 NBT 里的法术 ---------- */

    @Nullable
    private static String getSpellId(ItemStack scroll) {
        if (scroll.getTag() == null) return null;
        String id = scroll.getTag().getString(FlawlessMagicScroll.SPELL);
        return id.isEmpty() ? null : id;
    }
}
