package com.galtc2.compat.jerotes;

import com.galtc2.modifiers.QingfengModifier;
import com.jerotes.jerotes.entity.Other.OtherSpell.InsectPlagueEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 清枫特性的 J村 实现：直接复用 J村 原版「疫病虫群」法术的实体
 * （{@link InsectPlagueEntity}，法术 {@code jerotes:insect_plague}）。
 * <p>
 * 与 J村 原版 {@code SpellFind.InsectPlague} 的差别：
 * <ul>
 *   <li>不登记「施法专注」：被动特性不该被玩家之后的操作/受击打断</li>
 *   <li>存在时间按等级写死为 10 / 15 / 20 ... 秒：原版实体 {@code getMaxLife()} 是 12000 tick
 *       （10 分钟），这里把 lifeTick 直接推到末尾前 N 秒，并把 startTick 拉满以跳过 40 tick 的展开阶段</li>
 * </ul>
 * <b>法术等级保持原版不变</b>（{@code setSpellLevelDamage(getBaseSpellLevel())} 的值），
 * 逐次伤害口径因此与原版完全一致；攻击力成长改为在 {@link JerotesEvents} 里按原版 5 tick 节奏
 * 额外调用几次 {@code dealDamageTo}，次数由 {@link QingfengModifier#bites(int)} 决定，
 * 记在虫群实体的 {@link QingfengModifier#KEY_EXTRA_BITES} 上。
 * 仅在 J村 存在时由 {@link JerotesCompat} 注入到 {@link QingfengModifier}。
 */
final class JerotesInsectPlagueCaster implements QingfengModifier.Swarm {

    private static final int TICKS_PER_SECOND = 20;
    /** J村 原版 insect_plague 的法术等级（getBaseSpellLevel），保持不动 */
    private static final int BASE_SPELL_LEVEL = 5;
    /** 认领既有虫群的范围（格） */
    private static final double SEARCH_RANGE = 4.0D;

    @Override
    public boolean summon(LivingEntity caster, LivingEntity victim, int level) {
        if (caster == null || victim == null) return false;
        Level world = caster.level();
        if (!(world instanceof ServerLevel server)) return false;

        int alive = QingfengModifier.seconds(level) * TICKS_PER_SECOND;
        // 额外撕咬次数在 J村 原版那一次之外，所以减 1
        int extraBites = Math.max(0, QingfengModifier.bites(level) - 1);

        List<InsectPlagueEntity> swarms = findSwarms(world, caster, victim);
        if (swarms.isEmpty()) {
            InsectPlagueEntity cloud = new InsectPlagueEntity(world, caster);
            // 法术等级恒为原版：不放大法术等级，避免与 J村 后续的玩家法术体系互相干扰
            cloud.setSpellLevelDamage(BASE_SPELL_LEVEL);
            cloud.setPos(victim.getX() + 0.5D, victim.getY() + 0.125D, victim.getZ() + 0.5D);
            cloud.setOwnerUUID(caster.getUUID());
            server.addFreshEntity(cloud);
            swarms = findSwarms(world, caster, victim);
        }

        // 命中即把存在时间刷成当前等级的时长，并刷新额外撕咬次数
        for (InsectPlagueEntity swarm : swarms) {
            swarm.setStartTick(swarm.getMaxStart());
            swarm.setLifeTick(Math.max(swarm.getMaxStart(), swarm.getMaxLife() - alive));
            if (extraBites > 0) {
                swarm.getPersistentData().putInt(QingfengModifier.KEY_EXTRA_BITES, extraBites);
            } else {
                swarm.getPersistentData().remove(QingfengModifier.KEY_EXTRA_BITES);
            }
        }
        return true;
    }

    /** 取 victim 附近属于该召唤者的虫群（用于查重与刷新存在时间） */
    private static List<InsectPlagueEntity> findSwarms(Level world, LivingEntity caster, LivingEntity victim) {
        return world.getEntitiesOfClass(InsectPlagueEntity.class,
                victim.getBoundingBox().inflate(SEARCH_RANGE), swarm -> caster.equals(swarm.getOwner()));
    }
}
