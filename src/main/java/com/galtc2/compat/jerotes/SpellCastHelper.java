package com.galtc2.compat.jerotes;

import com.jerotes.jerotes.spell.MagicSpell;
import com.jerotes.jerotes.spell.MagicType;
import com.jerotes.jerotes.spell.SpellListByString;
import com.jerotes.jerotes.spell.SpellRegistry;
import com.jerotes.jerotes.spell.SpellType;
import com.jerotes.jerotes.spell.SpellTypeInterface;
import com.jerotes.jerotes.util.Main;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * 魔法施法接口（借用自 jvt 的 com.jvt.spell.SpellCastHelper）：封装 J村 魔法体系的调用。
 * <ul>
 *   <li>右键(castRightClick)：即时施放非近战法术，目标取自玩家视线</li>
 *   <li>左键(castMelee)：近战命中时施放近战型/自施型法术</li>
 *   <li>长按右键(startCharge/tickCharge/releaseCharge)：蓄力施放带施法时间或专注的法术</li>
 * </ul>
 * 底层统一走 {@link #cast}，内部处理冷却、目标选择、类型过滤与距离校验。
 * <p>
 * 仅在 J村 存在时被加载，见 {@link JerotesCompat}。
 */
public final class SpellCastHelper {

    /** 长按蓄力状态(玩家持久 NBT，galtc2 前缀避免与 J村/jvt 冲突) */
    private static final String KEY_CHARGE_SPELL = "galtc2_charge_spell";
    private static final String KEY_CHARGE_LEVEL = "galtc2_charge_level";
    private static final String KEY_CHARGE_TICK = "galtc2_charge_tick";

    /** 视线选择目标的默认距离 */
    private static final double TARGET_RANGE = 256.0D;

    private SpellCastHelper() {
    }

    /* ============ 右键：即时施法 ============ */

    /** 右键施放：非近战法术，目标按法术类型自动选择(自身/视线)。客户端直接返回 false。 */
    public static boolean castRightClick(String spellId, int level, Player player) {
        if (player.level().isClientSide()) return false;
        MagicSpell probe = makeSpell(spellId, level, player, player);
        if (probe == null) return false;
        if (probe.isMelee()) return false; // 近战法术走左键
        return cast(spellId, level, player, null);
    }

    /* ============ 左键：近战施法 ============ */

    /** 左键/近战施放：仅近战型(isMelee)或自施型(SELF)法术，目标为命中目标。 */
    public static boolean castMelee(String spellId, int level, LivingEntity attacker, LivingEntity target) {
        if (attacker.level().isClientSide()) return false;
        if (target == null) return false;
        MagicSpell probe = makeSpell(spellId, level, attacker, attacker);
        if (probe == null) return false;
        if (!probe.isMelee() && probe.getMagicType() != MagicType.SELF) return false;
        return cast(spellId, level, attacker, target);
    }

    /* ============ 长按右键：蓄力施法 ============ */

    /** 开始蓄力。仅接受带施法时间(getCastingTicks > 0)或专注(isConcentration)的法术。 */
    public static boolean startCharge(String spellId, int level, Player player) {
        if (player.level().isClientSide()) return false;
        if (isCharging(player)) return false;
        if (SpellType.getCastCooldownJerotes(player) > 0) return false;
        MagicSpell probe = makeSpell(spellId, level, player, player);
        if (probe == null) return false;
        if (probe.getCastingTicks() <= 0 && !probe.isConcentration()) return false;
        CompoundTag data = Main.getJerotesPersistentData(player);
        data.putString(KEY_CHARGE_SPELL, spellId);
        data.putInt(KEY_CHARGE_LEVEL, level);
        data.putInt(KEY_CHARGE_TICK, 0);
        return true;
    }

    /** 蓄力推进(每 tick 调用一次)。蓄力满后自动释放，返回是否已释放。 */
    public static boolean tickCharge(Player player) {
        if (player.level().isClientSide()) return false;
        if (!isCharging(player)) return false;
        CompoundTag data = Main.getJerotesPersistentData(player);
        String spellId = data.getString(KEY_CHARGE_SPELL);
        int level = data.getInt(KEY_CHARGE_LEVEL);
        int tick = data.getInt(KEY_CHARGE_TICK) + 1;
        data.putInt(KEY_CHARGE_TICK, tick);

        MagicSpell probe = makeSpell(spellId, level, player, player);
        if (probe == null) {
            cancelCharge(player);
            return false;
        }
        probe.CastingAbout(tick);
        if (tick >= Math.max(1, probe.getCastingTicks())) {
            return releaseCharge(player);
        }
        return false;
    }

    /** 松开释放：蓄力足够则施放，不足则中断。返回是否成功施放。 */
    public static boolean releaseCharge(Player player) {
        if (player.level().isClientSide()) return false;
        if (!isCharging(player)) return false;
        CompoundTag data = Main.getJerotesPersistentData(player);
        String spellId = data.getString(KEY_CHARGE_SPELL);
        int level = data.getInt(KEY_CHARGE_LEVEL);
        int tick = data.getInt(KEY_CHARGE_TICK);
        clearCharge(player);

        MagicSpell probe = makeSpell(spellId, level, player, player);
        if (probe == null) return false;
        if (tick < Math.max(1, probe.getCastingTicks())) {
            probe.spellCastingStop(tick);
            return false;
        }
        return cast(spellId, level, player, null);
    }

    /** 中断蓄力。 */
    public static void cancelCharge(Player player) {
        if (player.level().isClientSide()) return;
        CompoundTag data = Main.getJerotesPersistentData(player);
        if (data.contains(KEY_CHARGE_SPELL)) {
            MagicSpell probe = makeSpell(data.getString(KEY_CHARGE_SPELL), data.getInt(KEY_CHARGE_LEVEL), player, player);
            if (probe != null) probe.spellCastingStop(data.getInt(KEY_CHARGE_TICK));
        }
        clearCharge(player);
    }

    /* ============ 通用入口 ============ */

    /**
     * 通用施法：校验冷却与法术有效性，选择目标并调用 {@link MagicSpell#spellUse()}。
     * 仅服务端执行。explicitTarget 为 null 时按法术类型自动选目标。
     */
    public static boolean cast(String spellId, int level, LivingEntity caster, @Nullable Entity explicitTarget) {
        if (caster.level().isClientSide()) return false;
        if (spellId == null || spellId.isEmpty()) return false;
        if (!SpellRegistry.spellExists(spellId)) return false;
        if (SpellType.getCastCooldownJerotes(caster) > 0) return false;

        SpellTypeInterface type = SpellRegistry.getSpellTypeById(spellId);
        if (type == null) return false;

        MagicSpell probe = SpellListByString.getSpell(level, caster, caster, type);
        if (probe == null) return false;

        Entity target = resolveTarget(probe, caster, explicitTarget);
        if (target == null) target = caster;

        MagicSpell spell = SpellListByString.getSpell(level, caster, target, type);
        if (spell == null) return false;
        if (!spell.canUse()) return false;

        // 目标类法术需额外校验目标有效性
        if (probe.getMagicType() == MagicType.TARGET && target != caster && !spell.canUseToEntity(target)) {
            return false;
        }

        boolean success = spell.spellUse();
        if (success) {
            caster.swing(InteractionHand.MAIN_HAND, true);
        }
        return success;
    }

    /* ============ 辅助 ============ */

    /** 是否正在蓄力。 */
    public static boolean isCharging(Player player) {
        return Main.getJerotesPersistentData(player).contains(KEY_CHARGE_SPELL);
    }

    /** 构建法术实例；法术 ID 无效或不存在时返回 null。 */
    @Nullable
    public static MagicSpell makeSpell(String spellId, int level, LivingEntity caster, Entity target) {
        if (spellId == null || spellId.isEmpty()) return null;
        if (!SpellRegistry.spellExists(spellId)) return null;
        SpellTypeInterface type = SpellRegistry.getSpellTypeById(spellId);
        if (type == null) return null;
        return SpellListByString.getSpell(level, caster, target, type);
    }

    /** 目标选择：显式目标优先；自施型取施法者；否则取玩家视线目标，取不到则回退施法者。 */
    private static Entity resolveTarget(MagicSpell probe, LivingEntity caster, @Nullable Entity explicitTarget) {
        if (explicitTarget != null) return explicitTarget;
        if (probe.getMagicType() == MagicType.SELF) return caster;
        if (caster instanceof Player player) {
            Entity target = Main.getTargetedEntity(player, TARGET_RANGE, false, probe.mustTargetPickable());
            if (target != null) return target;
        }
        return caster;
    }

    private static void clearCharge(Player player) {
        CompoundTag data = Main.getJerotesPersistentData(player);
        data.remove(KEY_CHARGE_SPELL);
        data.remove(KEY_CHARGE_LEVEL);
        data.remove(KEY_CHARGE_TICK);
    }
}
