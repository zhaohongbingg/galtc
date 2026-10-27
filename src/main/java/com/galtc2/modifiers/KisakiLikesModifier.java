package com.galtc2.modifiers;

import com.galtc2.Register.galtc2Modifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 月社妃武器特性：可最喜欢你（Kisaki Likes You）
 *
 * 攻击向/增益：
 * - 反击：攻击近期伤害过自己的生物时，返还其造成的累计伤害的 30%（每级），结算后清除记仇
 * - 副手为空或同为月社妃材质时：主手武器持续回复耐久（每秒 0.5% 最大耐久），并给玩家生命恢复（每秒 1 点）
 */
public class KisakiLikesModifier extends BaseModifiers {

    /** 反击返还比例：每级 30% */
    private static final float RETALIATION_RATIO = 0.30f;
    /** 记仇有效时间：5 分钟（6000 tick） */
    private static final long RECALL_TICKS = 6000L;
    /** 每秒回复生命值 */
    private static final float HEAL_PER_SECOND = 1.0f;
    /** 每秒回复耐久占最大耐久的比例 */
    private static final float REPAIR_PERCENT = 0.005f;

    /** 玩家 → 生物 → 记仇数据（累计伤害 + 最近一次被伤害时间） */
    private static final Map<UUID, Map<UUID, DamageRecord>> DAMAGE_RECORDS = new ConcurrentHashMap<>();

    private record DamageRecord(float cumulative, long lastTime) {}

    public KisakiLikesModifier() {
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDeath);
    }

    /* ===== 记仇：记录近期伤害过自己的生物 ===== */

    private void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAmount() <= 0) return;
        LivingEntity attacker = getSourceLiving(event.getSource());
        if (attacker == null) return;
        long time = player.level().getGameTime();
        DAMAGE_RECORDS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
                .compute(attacker.getUUID(), (k, old) -> {
                    if (old == null || time - old.lastTime > RECALL_TICKS) {
                        return new DamageRecord(event.getAmount(), time);
                    }
                    return new DamageRecord(old.cumulative + event.getAmount(), time);
                });
    }

    /** 玩家死亡后清空记仇 */
    private void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof Player player) {
            DAMAGE_RECORDS.remove(player.getUUID());
        }
    }

    /** 解析伤害来源（含投射物所有者） */
    private static LivingEntity getSourceLiving(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof LivingEntity living) return living;
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) return owner;
        if (source.getEntity() instanceof LivingEntity living) return living;
        return null;
    }

    /* ===== 反击：攻击记仇生物时返还累计伤害百分比 ===== */

    @Override
    public float onModifyMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                     LivingEntity attacker, LivingEntity target, float baseDamage, float actualDamage) {
        if (!(attacker instanceof Player player)) return actualDamage;
        if (target.isDeadOrDying()) return actualDamage;
        float bonus = consumeRetaliation(player, target, modifier.getLevel());
        return actualDamage + bonus;
    }

    /**
     * 箭矢命中：把「反击」搬到远程——射中近期伤害过自己的生物时，返还其累计伤害的
     * {@value #RETALIATION_RATIO} × 等级（与近战同一条 {@link #consumeRetaliation}，结算即清记仇）。
     * <p>
     * 追加伤害走 {@code attackEntitySecondary}（不碰 {@code target.hurt}，否则箭矢自身伤害会被无敌帧吞掉），
     * 来源挂箭矢，击杀统计仍归玩家。
     */
    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level,
                                      Projectile projectile, AbstractArrow arrow, EntityHitResult hit,
                                      LivingEntity attacker, LivingEntity target) {
        if (level <= 0 || attacker == null || target == null) return;
        if (attacker.level().isClientSide() || target.isDeadOrDying()) return;
        if (!(attacker instanceof Player player)) return;
        if (!(projectile instanceof AbstractArrow)) return;

        float bonus = consumeRetaliation(player, target, level);
        if (bonus <= 0f) return;

        ToolAttackUtil.attackEntitySecondary(player.damageSources().arrow(arrow, player),
                bonus, target, target, true);
    }

    /** 结算反击：取出并清除记仇，返回额外伤害 */
    private static float consumeRetaliation(Player player, LivingEntity target, int level) {
        Map<UUID, DamageRecord> records = DAMAGE_RECORDS.get(player.getUUID());
        if (records == null) return 0f;
        DamageRecord record = records.remove(target.getUUID());
        if (record == null) return 0f;
        long gameTime = player.level().getGameTime();
        if (gameTime - record.lastTime > RECALL_TICKS) return 0f;
        return record.cumulative * RETALIATION_RATIO * level;
    }

    /* ===== 副手增益：空手或同为月社妃材质 → 回复耐久 + 生命恢复 ===== */

    @Override
    public void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level,
                                        LivingEntity holder, int itemSlot, boolean isSelected,
                                        boolean isCorrectSlot, ItemStack itemStack) {
        if (level.isClientSide()) return;
        if (!isSelected) return;      // 仅主手持有时生效
        if (!(holder instanceof Player player)) return;
        if (tool.isBroken()) return;
        ItemStack offhand = holder.getOffhandItem();
        if (!offhand.isEmpty() && !isKisakiMaterial(offhand)) return; // 副手异材质武器由「可最讨厌你」处理
        if (level.getGameTime() % 20 != 0) return;

        // 主手武器回复耐久（每秒 0.5% 最大耐久）
        if (tool.getDamage() > 0) {
            int maxDurability = (int) (float) tool.getStats().get(ToolStats.DURABILITY);
            int repair = Math.max(1, (int) (maxDurability * REPAIR_PERCENT));
            ToolDamageUtil.repair(tool, repair);
        }
        // 生命恢复（每秒 1 点）
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(HEAL_PER_SECOND);
        }
    }

    /* ===== 工具方法 ===== */

    /** 判断手持物是否为月社妃材质（武器带 kisaki_likes/kisaki_hates，防具带 kisaki） */
    public static boolean isKisakiMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof ModifiableItem) && !(stack.getItem() instanceof ModifiableArmorItem)) return false;
        IToolStackView tool = ToolStack.from(stack);
        return tool.getModifierLevel(galtc2Modifier.kisaki.get()) > 0
                || tool.getModifierLevel(galtc2Modifier.kisaki_likes.get()) > 0
                || tool.getModifierLevel(galtc2Modifier.kisaki_hates.get()) > 0;
    }
}
