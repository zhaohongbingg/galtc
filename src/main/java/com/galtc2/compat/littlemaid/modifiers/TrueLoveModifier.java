package com.galtc2.compat.littlemaid.modifiers;

import com.galtc2.modifiers.BaseModifiers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 挚爱 (true_love) —— 车万女仆 联动远程词条（材料 traits）。
 * <p>
 * 效果：<b>箭矢命中后，让目标在 5 秒内优先索敌「除了玩家与酒狐以外的生物」，对 boss 不生效</b>。
 * <ul>
 *   <li>命中一只普通怪物 → 把它当前的仇恨目标改写成它身边的另一只生物（优先挑怪物），
 *       并让它把那只生物当成「刚打过自己的家伙」，于是会追着打而不是继续追玩家；</li>
 *   <li>5 秒内每 {@value #REFRESH_INTERVAL} tick 重设一次目标 —— 原版 AI 有可能自己把目标清掉，
 *       只设一次的话经常一两秒就失效；到期后不再干预，怪物恢复正常索敌；</li>
 *   <li>重定向目标死了 / 不在附近 / 变成了不该打的对象（玩家、酒狐、boss）时直接放弃，
 *       不去「补一个新的」—— 免得把词条变成永久的仇恨洗牌。</li>
 * </ul>
 * 判定口径：
 * <ul>
 *   <li>只会对 {@link Mob} 生效（史莱姆、苦力怕这类没有主动索敌的也行，只是看不出来）；</li>
 *   <li>boss 走 {@code forge:bosses} 标签判断（见 {@link JiuHuHelper#isBoss});</li>
 *   <li>「酒狐」= 玩家的车万女仆 女仆（见 {@link JiuHuHelper#isJiuHu}），玩家以外的友军不特殊处理。</li>
 * </ul>
 */
public class TrueLoveModifier extends BaseModifiers {

    /** 索敌转移持续时间：5 秒 */
    private static final int DURATION = 100;

    /** 期间每隔多少 tick 重设一次目标 */
    private static final int REFRESH_INTERVAL = 10;

    /** 找替罪羊的搜索半径（格） */
    private static final double SEARCH_RANGE = 16.0D;

    /** 怪物 UUID → 被强制索敌的目标 */
    private static final Map<UUID, Redirect> REDIRECTS = new ConcurrentHashMap<>();

    /** @param targetId 被强制索敌的目标实体 id；@param expireAt 失效的游戏刻 */
    private record Redirect(int targetId, long expireAt) {
    }

    public TrueLoveModifier() {
        MinecraftForge.EVENT_BUS.addListener(this::onLivingTick);
    }

    /* ------------------------------------------------------------------ 命中 */

    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level,
                                      Projectile projectile, AbstractArrow arrow, EntityHitResult hit,
                                      LivingEntity attacker, LivingEntity target) {
        if (!(target instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        // 对 boss 不生效
        if (JiuHuHelper.isBoss(mob)) {
            return;
        }
        LivingEntity bait = findBait(mob, attacker);
        if (bait == null) {
            return;
        }
        mob.setTarget(bait);
        mob.setLastHurtByMob(bait);
        REDIRECTS.put(mob.getUUID(), new Redirect(bait.getId(), mob.level().getGameTime() + DURATION));
    }

    /**
     * 挑一只「除了玩家与酒狐以外」的生物当仇恨替身：
     * 优先主动敌对生物（{@link Enemy}，打起来才有意义），同类里取最近的。
     */
    @Nullable
    private static LivingEntity findBait(Mob mob, @Nullable LivingEntity attacker) {
        List<LivingEntity> candidates = new ArrayList<>(mob.level().getEntitiesOfClass(LivingEntity.class,
                mob.getBoundingBox().inflate(SEARCH_RANGE),
                candidate -> isValidBait(mob, attacker, candidate)));
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator
                .comparingInt((LivingEntity candidate) -> candidate instanceof Enemy ? 0 : 1)
                .thenComparingDouble(candidate -> candidate.distanceToSqr(mob)));
        return candidates.get(0);
    }

    private static boolean isValidBait(Mob mob, @Nullable LivingEntity attacker, LivingEntity candidate) {
        if (candidate == mob || candidate == attacker || !candidate.isAlive() || candidate.isSpectator()) {
            return false;
        }
        if (candidate instanceof Player || JiuHuHelper.isJiuHu(candidate) || JiuHuHelper.isBoss(candidate)) {
            return false;
        }
        return true;
    }

    /* ------------------------------------------------------------------ 维持 */

    private void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (REDIRECTS.isEmpty() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        Level level = mob.level();
        if (level.isClientSide()) {
            return;
        }
        Redirect redirect = REDIRECTS.get(mob.getUUID());
        if (redirect == null) {
            return;
        }
        long now = level.getGameTime();
        if (now >= redirect.expireAt() || !mob.isAlive()) {
            REDIRECTS.remove(mob.getUUID());
            return;
        }
        if (now % REFRESH_INTERVAL != 0) {
            return;
        }
        Entity entity = level.getEntity(redirect.targetId());
        if (!(entity instanceof LivingEntity bait) || !isValidBait(mob, null, bait)) {
            REDIRECTS.remove(mob.getUUID());
            return;
        }
        mob.setTarget(bait);
    }

    /* ------------------------------------------------------------------ 等级 */

    /** 单级词条：不显示等级 */
    @Override
    public boolean isSingleLevel() {
        return true;
    }
}
