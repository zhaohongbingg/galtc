package com.galtc2.compat.jerotes;

import com.galtc2.modifiers.EuthanasiaModifier;
import com.jerotes.jerotesvillage.entity.Shoot.Magic.Cloud.UncleanBloodRainEntity;
import com.jerotes.jerotesvillage.spell.OtherSpellFind;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 腐姬特性的 J村 实现：直接复用 J村 原版「不洁血雨」法术的效果实现
 * （{@link OtherSpellFind#UncleanBloodRain}，法术 {@code jerotesvillage_unclean_blood_rain}）。
 * <p>
 * 随匠魂等级成长（1 级即 J村 原版数值，之后每级翻倍）：
 * <ul>
 *   <li>雨云半径：{@code 1.5→3.0} × 2^(等级-1) 格，上限 32 格</li>
 *   <li>伤害：原版伤害来自「不洁之身」效果（每生效一跳造成 {@code 效果等级} 点魔法伤害，
 *       而效果等级 = 传入的法术等级），所以法术等级按 {@code 3 × 2^(等级-1)} 放大即等同于伤害翻倍</li>
 * </ul>
 * 另外把雨云与「不洁之身」的持续时间固定为 {@link #CLOUD_SECONDS} 秒
 * （原版 = 6 * 法术等级，3 级为 18 秒）。
 * <p>
 * 半径在原版方法里写死、参数又没暴露，所以只能在法术生成雨云之后再修正一次。
 * 仅在 J村 存在时由 {@link JerotesCompat} 注入到 {@link EuthanasiaModifier}。
 */
final class JerotesBloodRainCaster implements EuthanasiaModifier.BloodRain {

    /** 1 级时的法术等级（J村 原版 unclean_blood_rain 的 getBaseSpellLevel） */
    private static final int BASE_SPELL_LEVEL = 3;
    /** 雨云与「不洁之身」的持续时间（秒） */
    private static final int CLOUD_SECONDS = 15;
    /** 每次特性触发降下的雨云数量 */
    private static final int CLOUD_COUNT = 1;

    /** 1 级时的雨云初始半径（格），J村 原版写死 1.5 */
    private static final float BASE_RADIUS = 1.5f;
    /** 1 级时的雨云终半径（格），J村 原版写死 3.0 */
    private static final float BASE_MAX_RADIUS = 3.0f;
    /** 半径上限（J村 setRadius 内部会把半径 clamp 到 32 格） */
    private static final float RADIUS_LIMIT = 32.0f;
    /** 法术等级上限（放大器 = 等级-1，需落在 MobEffect 允许范围内） */
    private static final int SPELL_LEVEL_LIMIT = 255;

    @Override
    public boolean rain(LivingEntity caster, LivingEntity victim, int level) {
        if (caster == null || victim == null) return false;
        Level world = caster.level();
        if (world.isClientSide()) return false;

        int lv = Math.max(1, level);
        double scale = Math.pow(2.0D, lv - 1);
        float radius = (float) Math.min(RADIUS_LIMIT, BASE_RADIUS * scale);
        float maxRadius = (float) Math.min(RADIUS_LIMIT, BASE_MAX_RADIUS * scale);
        int spellLevel = (int) Math.min(SPELL_LEVEL_LIMIT, BASE_SPELL_LEVEL * scale);

        if (!findClouds(world, caster, victim, maxRadius + 4.0D).isEmpty()) return true;

        if (!OtherSpellFind.UncleanBloodRain(caster, victim, CLOUD_SECONDS, CLOUD_SECONDS, spellLevel,
                CLOUD_COUNT, victim.getBbHeight() + 2.0f)) {
            return false;
        }

        // 原版把半径写死为 1.5 → 3.0，这里按等级放大并同步碰撞箱（碰撞箱决定影响范围）
        for (UncleanBloodRainEntity cloud : findClouds(world, caster, victim, maxRadius + 4.0D)) {
            cloud.setRadius(radius);
            cloud.setRadiusPerTick((maxRadius - radius) / Math.max(1, cloud.getDuration()));
            cloud.refreshDimensions();
        }
        return true;
    }

    /** 取 victim 附近属于该施法者的雨云（施法前用于查重，施法后用于放大范围） */
    private static List<UncleanBloodRainEntity> findClouds(Level world, LivingEntity caster, LivingEntity victim,
                                                           double range) {
        return world.getEntitiesOfClass(UncleanBloodRainEntity.class,
                victim.getBoundingBox().inflate(range), cloud -> caster.equals(cloud.getOwner()));
    }
}
