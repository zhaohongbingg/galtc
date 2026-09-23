package com.galtc2.compat.jerotes;

import com.galtc2.modifiers.SayaModifier;
import com.jerotes.jerotes.util.Main;
import com.jerotes.jerotesvillage.entity.Other.MagicAboutMob.UncleanTentacleEntity;
import com.jerotes.jerotesvillage.init.JerotesVillageEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 沙耶特性的触手召唤实现：生成 J村 的 {@code unclean_tentacle} 并归属攻击者。
 * <p>
 * 仅在 J村 存在时由 {@link JerotesCompat} 注入到 {@link SayaModifier}。
 */
final class JerotesTentacleSummoner implements SayaModifier.TentacleSummoner {

    @Override
    public void summon(Level level, LivingEntity attacker, LivingEntity target, float damage) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        RandomSource random = level.random;
        BlockPos pos = target.blockPosition().offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);

        UncleanTentacleEntity tentacle = JerotesVillageEntityType.UNCLEAN_TENTACLE.get()
                .spawn(serverLevel, pos, MobSpawnType.MOB_SUMMONED);
        if (tentacle == null) return;

        Main.conjureAbout(attacker, tentacle);
        tentacle.setOwner(attacker);
        tentacle.setSpellLevelDamage(4);
        if (tentacle.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            tentacle.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        }
        if (target != attacker) {
            tentacle.setTarget(target);
        }
    }

    @Override
    public void summonDefense(Level level, LivingEntity defender, LivingEntity attacker, float damage) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos pos = defender.blockPosition().offset(level.random.nextInt(3) - 1, 0, level.random.nextInt(3) - 1);
        UncleanTentacleEntity tentacle = JerotesVillageEntityType.UNCLEAN_TENTACLE.get()
                .spawn(serverLevel, pos, MobSpawnType.MOB_SUMMONED);
        if (tentacle == null) return;

        Main.conjureAbout(defender, tentacle);
        tentacle.setOwner(defender);
        tentacle.setSpellLevelDamage(4);
        if (tentacle.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            tentacle.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(20.0f);
        }
        tentacle.setTarget(attacker);
    }
}
