package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.init.JerotesMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 奴制监督者·捆缚(chain_bind)
 * 近战命中后 40% 几率给目标施加 J村 的麻醉状态(jerotes:anesthetized)，
 * 效果等级与持续时间随强化等级提升，累积到阈值后由 J村 自身转为麻醉定身。
 */
public class ChainBindModifier extends Modifier implements MeleeHitModifierHook {

    private static final float CHANCE = 0.4f;
    /** 每级麻醉持续时间（tick），1 级 = 10 秒 */
    private static final int DURATION_PER_LEVEL = 200;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target == null || attacker == null || !target.isAlive()) {
            return;
        }
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return;
        }
        if (level.getRandom().nextFloat() > CHANCE) {
            return;
        }

        int modLevel = modifier.getLevel();
        // 效果等级从 0（麻醉 I）起算，与 J村 麻醉投团的麻醉值档位一致
        target.addEffect(new MobEffectInstance(
                JerotesMobEffects.ANESTHETIZED.get(),
                DURATION_PER_LEVEL * modLevel,
                Math.max(0, modLevel - 1),
                false,
                true,
                true));
    }
}
