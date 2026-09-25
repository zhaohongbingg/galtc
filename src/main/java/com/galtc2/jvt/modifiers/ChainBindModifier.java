package com.galtc2.jvt.modifiers;

import com.jerotes.jerotes.util.Main;
import com.jerotes.jerotesvillage.entity.Other.MagicAboutMob.AnimatedChainEntity;
import com.jerotes.jerotesvillage.init.JerotesVillageEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 奴制监督者·捆缚(chain_bind)
 * 复刻 J村 奴制监督者锤鞭：近战命中后 40% 几率，把生命值不高于自身 2 倍的生物用 {@link AnimatedChainEntity} 捆住。
 */
public class ChainBindModifier extends Modifier implements MeleeHitModifierHook {

    private static final float CHANCE = 0.4f;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target == null || !target.isAlive()) {
            return;
        }
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getRandom().nextFloat() > CHANCE) {
            return;
        }
        if (target.getHealth() > attacker.getHealth() * 2.0f) {
            return;
        }
        if (!Main.mobSizeSmall(target) && !Main.mobSizeMedium(target) && !Main.mobSizeLarge(target)) {
            return;
        }
        if (target instanceof AnimatedChainEntity) {
            return;
        }

        AnimatedChainEntity chain = JerotesVillageEntityType.ANIMATED_CHAIN.get()
                .spawn(serverLevel, BlockPos.containing(target.getX(), target.getY(), target.getZ()), MobSpawnType.MOB_SUMMONED);
        if (chain != null) {
            chain.setOwner(attacker);
            chain.setPrisoner(target);
            chain.setSize(Main.mobWidth(target));
            Team team = attacker.getTeam();
            if (team instanceof PlayerTeam playerTeam) {
                serverLevel.getScoreboard().addPlayerToTeam(chain.getStringUUID(), playerTeam);
            }
            chain.setTarget(target);
        }
        serverLevel.gameEvent(GameEvent.ENTITY_PLACE, BlockPos.containing(attacker.getX(), attacker.getY(), attacker.getZ()), GameEvent.Context.of(attacker));
    }
}
