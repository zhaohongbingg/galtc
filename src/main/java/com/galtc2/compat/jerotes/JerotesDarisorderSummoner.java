package com.galtc2.compat.jerotes;

import com.galtc2.modifiers.ShoujingModifier;
import com.jerotes.jerotesvillage.entity.MagicSummoned.Darisorder.DarisorderEntity;
import com.jerotes.jerotesvillage.spell.OtherSpellFind;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 勺景特性的召唤实现：复用 J村 原版「呼唤暗乱」法术的效果实现
 * （{@link OtherSpellFind#CallTheDarisorder}，法术 {@code jerotesvillage_call_the_darisorder}）。
 * <p>
 * 原版法术在 {@code OtherSpellList.CallTheDarisorder} 中的调用为
 * {@code CallTheDarisorder(caster, 1, 1, 16)}：在施法者前方/附近召唤 1 只暗乱，
 * 归属施法者、非永久、30 秒后消失，并登记专注。这里保持相同参数，
 * 只额外统计周围一定范围内的暗乱数量，达到上限就不再召唤。
 * <p>
 * 仅在 J村 存在时由 {@link JerotesCompat} 注入到 {@link ShoujingModifier}。
 */
final class JerotesDarisorderSummoner implements ShoujingModifier.Summoner {

    /** 一次呼唤的暗乱数量（原版为 1） */
    private static final int MIN_COUNT = 1;
    private static final int MAX_COUNT = 1;
    /** 召唤距离：玩家沿视线前方，其它生物在自身附近 */
    private static final int SPAWN_DISTANCE = 16;
    /** 范围内暗乱数量上限，达到上限则不再召唤（J村 原版法术的 canUse 判定为 2） */
    private static final int MAX_ACTIVE = 3;
    private static final double COUNT_RANGE = 24.0D;

    @Override
    public void summon(Level level, LivingEntity owner, LivingEntity target) {
        if (owner == null || level.isClientSide()) return;

        // 统计范围内被召唤出来的暗乱（非永久体），数量达标就不再召唤
        List<DarisorderEntity> alive = level.getEntitiesOfClass(DarisorderEntity.class,
                owner.getBoundingBox().inflate(COUNT_RANGE), DarisorderEntity::isNotPersistent);
        if (alive.size() >= MAX_ACTIVE) return;

        OtherSpellFind.CallTheDarisorder(owner, MIN_COUNT, MAX_COUNT, SPAWN_DISTANCE);
    }
}
