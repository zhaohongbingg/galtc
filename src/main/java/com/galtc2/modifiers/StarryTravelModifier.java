package com.galtc2.modifiers;

import com.galtc2.GaltcConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.List;

public class StarryTravelModifier extends Modifier implements MeleeDamageModifierHook, TooltipModifierHook, ProjectileHitModifierHook {

    /**
     * 每支箭只掷一次暴击骰：穿射箭每命中一个实体都会再派发一次 {@code PROJECTILE_HIT}，
     * 没有这个标记会变成"每穿一只重新 roll 一遍"。
     */
    private static final ResourceLocation KEY_ARROW_CRIT_DONE = ResourceLocation.fromNamespaceAndPath("galtc2", "starry_arrow_crit_done");

    /** 每一份「区块数 / CHUNKS_PER_DAMAGE」换算出来的暴击率：默认 5 区块 → +10% */
    private static final double CRIT_CHANCE_PER_STEP = 0.10D;

    /**
     * 暴击伤害的基础追加比例：<b>近处 = +50%</b>（也就是「暴击那一下 ×1.5」）。
     * <p>
     * 口径沿革：0.25 曾是原版暴击期望（原版暴击是「额外 0..damage/2+1」，期望约 +25%）；
     * 0.5 是用户 2026-10-27 要求的加强版起点。现在它只是<b>成长曲线的起点</b>，随距离继续往上加
     * （见 {@link #CRIT_BONUS_PER_STEP} / {@link #CRIT_BONUS_MAX}）。
     */
    private static final double CRIT_BONUS_BASE = 0.5D;

    /**
     * 每「一档距离」额外增加的爆伤：<b>+10%</b>，档位与暴击率同一把尺子
     * （1 档 = {@code STARRY_TRAVEL_CHUNKS_PER_DAMAGE} 区块，默认 5 区块）。
     */
    private static final double CRIT_BONUS_PER_STEP = 0.10D;

    /**
     * 爆伤封顶：<b>+200%</b>（乘 3 倍那一下）。
     * <p>
     * 成长曲线（默认每档 5 区块）：
     * <pre>
     *   步数 = 0（0 区块） → +50%   （0.5 + 0.1×0）
     *   步数 = 6（30 区块）→ +110%  （0.5 + 0.1×6）
     *   步数 = 10（50 区块）→ +150% （0.5 + 0.1×10）
     *   步数 = 15（75 区块）→ +200% （0.5 + 1.5 = 2.0，正好触顶）
     * </pre>
     */
    private static final double CRIT_BONUS_MAX = 2.0D;

    /**
     * 按区块距离算当前爆伤追加比例：{@code min(MAX, BASE + PER_STEP × steps)}，{@code steps = chunks / CHUNKS_PER_DAMAGE}。
     * <p>
     * {@code chunksPerDamage <= 0} 时退化为基础值，避免除零。
     */
    private static double critBonusFor(int chunks, int chunksPerDamage) {
        if (chunksPerDamage <= 0) {
            return CRIT_BONUS_BASE;
        }
        double steps = (double) chunks / chunksPerDamage;
        return Math.min(CRIT_BONUS_MAX, CRIT_BONUS_BASE + CRIT_BONUS_PER_STEP * steps);
    }

    public StarryTravelModifier() {}

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.TOOLTIP, ModifierHooks.PROJECTILE_HIT);
    }

    private int getDistanceInChunks(LivingEntity entity) {
        BlockPos spawnPos = entity.level().getSharedSpawnPos();
        if (spawnPos == null) {
            spawnPos = new BlockPos(0, 64, 0);
        }
        BlockPos entityPos = entity.blockPosition();
        int dx = Math.abs(entityPos.getX() - spawnPos.getX()) / 16;
        int dz = Math.abs(entityPos.getZ() - spawnPos.getZ()) / 16;
        return Math.max(dx, dz);
    }

    public static int getArmorBonus(LivingEntity entity) {
        BlockPos spawnPos = entity.level().getSharedSpawnPos();
        if (spawnPos == null) {
            spawnPos = new BlockPos(0, 64, 0);
        }
        BlockPos entityPos = entity.blockPosition();
        int dx = Math.abs(entityPos.getX() - spawnPos.getX()) / 16;
        int dz = Math.abs(entityPos.getZ() - spawnPos.getZ()) / 16;
        int chunks = Math.max(dx, dz);
        int chunksPerArmor = GaltcConfig.COMMON.STARRY_TRAVEL_CHUNKS_PER_ARMOR.get();
        return chunks / chunksPerArmor;
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if (context.getAttacker() == null || context.getAttacker().level().isClientSide()) return damage;
        int chunks = getDistanceInChunks(context.getAttacker());
        int chunksPerDamage = GaltcConfig.COMMON.STARRY_TRAVEL_CHUNKS_PER_DAMAGE.get();
        float bonus = (float) chunks / chunksPerDamage;
        return damage + bonus;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (player != null) {
            int chunks = getDistanceInChunks(player);
            int chunksPerDamage = GaltcConfig.COMMON.STARRY_TRAVEL_CHUNKS_PER_DAMAGE.get();
            int chunksPerArmor = GaltcConfig.COMMON.STARRY_TRAVEL_CHUNKS_PER_ARMOR.get();
            float damageBonus = (float) chunks / chunksPerDamage;
            int armorBonus = chunks / chunksPerArmor;
            double critChance = chunksPerDamage > 0
                    ? Math.min(1.0D, ((double) chunks / chunksPerDamage) * CRIT_CHANCE_PER_STEP)
                    : 0.0D;
            tooltip.add(Component.literal("§d当前距离: " + chunks + " 区块"));
            tooltip.add(Component.literal("§d近战加成: +" + String.format("%.1f", damageBonus) + " 伤害"));
            // 远程口径（用户 2026-10-27）：暴击率与爆伤都随距离成长（每 5 区块一档）
            tooltip.add(Component.literal("§d箭矢暴击率: +" + String.format("%.0f", critChance * 100.0D)
                    + "%（每 " + chunksPerDamage + " 区块 +10%），暴击伤害 +"
                    + String.format("%.0f", critBonusFor(chunks, chunksPerDamage) * 100.0D) + "%"));
            tooltip.add(Component.literal("§d盔甲加成: +" + armorBonus + " 护甲"));
        }
    }

    /**
     * 箭矢命中：<b>不再加基础伤害</b>，改成按「离世界出生点的区块距离」掷骰提高<b>暴击率</b>
     * （用户 2026-10-27 口径：每 5 区块 +10%）。
     * <p>
     * 为什么换掉原来的 {@code arrow.setBaseDamage(base + bonus)}：箭的最终伤害是
     * {@code baseDamage × 速度 × 暴击}，而星旅弓的基础伤害只有 ≈2.0，+6 的固定加成会被速度与暴击
     * 放大约 4.7 倍（F-032 实测 +28.58，理论 +6.1）。改成暴击骰后加成只作用在「是否暴击」这一个维度上。
     * <p>
     * 公式：{@code critChance = min(1.0, (区块数 / CHUNKS_PER_DAMAGE) × 0.10)}（默认 5 区块 → +10%，
     * 30 区块 60%，50 区块封顶 100%）。{@code level} 不参与：暴击率只由距离决定。
     * <p>
     * 掷中后统一追加一次 {@code baseDamage × critBonus}（近处 ×0.5 = 1.5 倍那一下，随距离继续涨到 ×2.0 = 3 倍）：
     * <ul>
     *   <li>爆伤成长：{@code critBonus = min(2.0, 0.5 + 0.1 × (chunks / CHUNKS_PER_DAMAGE))}，
     *       与暴击率用同一档位（默认 5 区块 1 档）：0 区块 +50%、30 区块 +110%、50 区块 +150%、75 区块触顶 +200%；</li>
     *   <li>走匠魂的二次伤害入口 {@code attackEntitySecondary}，不是 {@code target.hurt()}：
     *       它会临时把 {@code invulnerableTime} 清零、结算后还原，所以箭矢自身伤害照常结算；</li>
     *   <li>不再写 {@code arrow.setCritArrow(true)}：原版暴击是随机的 0..damage/2+1（期望 +25%），
     *       若既打标记又追加就是<b>双计</b>；而只打标记又给不出用户要的爆伤。
     *       统一成一条追加路径后，同一发只吃一次爆伤，且与蓄力程度无关；</li>
     *   <li>每支箭只掷一次（{@value #KEY_ARROW_CRIT_DONE}），穿射不会反复 roll、也不会重复追加。</li>
     * </ul>
     * 全程不调 {@code target.hurt()}：那会把 {@code invulnerableTime} 顶满、吃掉箭矢自身伤害。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null) return false;
        if (attacker.level().isClientSide()) return false;
        if (!(projectile instanceof AbstractArrow arrow) || arrow.getBaseDamage() <= 0.0D) return false;
        if (target.isDeadOrDying()) return false;
        if (persistentData.contains(KEY_ARROW_CRIT_DONE)) return false;

        int chunksPerDamage = GaltcConfig.COMMON.STARRY_TRAVEL_CHUNKS_PER_DAMAGE.get();
        if (chunksPerDamage <= 0) return false;
        int chunks = getDistanceInChunks(attacker);
        double critChance = Math.min(1.0D, ((double) chunks / chunksPerDamage) * CRIT_CHANCE_PER_STEP);
        if (critChance <= 0.0D) return false;

        // 每支箭只掷一次：先记账，无论中不中都不再重掷
        persistentData.putBoolean(KEY_ARROW_CRIT_DONE, true);
        if (attacker.getRandom().nextDouble() >= critChance) return false;

        // 掷中：统一追加 baseDamage × critBonus（爆伤随距离成长；只此一条路径，不叠加原版暴击）
        double critBonus = critBonusFor(chunks, chunksPerDamage);
        ToolAttackUtil.attackEntitySecondary(attacker.damageSources().arrow(arrow, attacker),
                (float) (arrow.getBaseDamage() * critBonus), target, target, true);
        return false;
    }
}
