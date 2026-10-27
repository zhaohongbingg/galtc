package com.galtc2.jvt.modifiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

/**
 * 村民金属·特性(村民之力)
 * 附近每只村民提升 5% 攻击(近战伤害)与 5% 防御(受到伤害减免)。
 * 采用按需检测：只在攻击结算/受伤结算时统计附近村民，不做每 tick 扫描。
 * <p>
 * <b>箭矢分支（F-029 → F-031 调整）</b>：远程命中追加 <b>{@code 村民数 × 等级}</b> 的
 * <b>品脱（flat）伤害</b>——用户口径是「这个材料和村民有关系，所以是周围村民的数量」，
 * 所以不再按箭伤百分比，而是直接按周围村民只数（16 格内，上限 {@value #MAX_VILLAGERS}）× 强化等级加固定伤害；
 * 追加伤害走 {@link ToolAttackUtil#attackEntitySecondary}，不在 hook 里 {@code target.hurt()}。
 * hook 只由攻击侧的 {@link VillagerMetalMeleeModifier} 注册（护甲侧的
 * {@link VillagerMetalArmorModifier} 不注册，所以远程加成不会因为穿护甲而叠进来）。
 * 每支箭对同一目标只结算一次（箭矢 {@code persistentData} 标记）。
 * 近战（{@code MELEE_DAMAGE} 的 +5%/村民）与防御（{@code MODIFY_HURT} 的 -5%/村民）两段不受本调整影响。
 */
public class VillagerMetalModifier extends Modifier implements MeleeDamageModifierHook, ModifyDamageModifierHook, ProjectileHitModifierHook {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };
    /** 检测半径(格) */
    private static final double RADIUS = 16.0;
    /** 每只村民的加成 */
    private static final float BONUS_PER_VILLAGER = 0.05f;
    /** 计入的村民上限(避免村庄刷屏时数值爆炸) */
    private static final int MAX_VILLAGERS = 16;
    /** 防御减伤封顶 */
    private static final float MAX_DEFENSE_REDUCTION = 0.8f;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.MODIFY_HURT);
    }

    /** 攻击：加算 +5%/村民 */
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        int villagers = countVillagers(context.getAttacker());
        if (villagers <= 0) {
            return damage;
        }
        return damage * (1.0f + BONUS_PER_VILLAGER * villagers);
    }

    /** 防御：减伤 5%/村民，封顶 80%，只结算一次(与穿戴件数无关) */
    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirect) {
        if (amount <= 0) {
            return amount;
        }
        EquipmentSlot firstSlot = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            IToolStackView armor = context.getToolInSlot(slot);
            if (armor != null && !armor.isBroken() && armor.getModifierLevel(this) > 0) {
                firstSlot = slot;
                break;
            }
        }
        if (firstSlot == null || slotType != firstSlot) {
            return amount;
        }
        int villagers = countVillagers(context.getEntity());
        if (villagers <= 0) {
            return amount;
        }
        float reduction = Math.min(BONUS_PER_VILLAGER * villagers, MAX_DEFENSE_REDUCTION);
        return amount * (1.0f - reduction);
    }

    /**
     * 箭矢命中：追加 {@code 村民数 × 等级} 的品脱伤害（村民数 16 格内、上限 {@value #MAX_VILLAGERS}）。
     * <p>
     * 与近战 {@code ×(1 + 5% × 村民数)} 的百分比口径<b>不同</b>：F-031 按用户口改为按"周围村民的数量"
     * 直接加固定点数（1 级 = 每只村民 1 点，2 级 = 每只村民 2 点）。
     */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         LivingEntity attacker, LivingEntity target) {
        // 服务端权威 + 空值/存活守卫
        if (projectile.level().isClientSide() || target == null || attacker == null || !target.isAlive()) {
            return false;
        }
        if (!(projectile instanceof AbstractArrow arrow)) {
            return false;
        }
        int level = modifier.getLevel();
        if (level <= 0) {
            return false;
        }
        int villagers = countVillagers(attacker);
        if (villagers <= 0) {
            return false;
        }
        // 品脱伤害 = 村民数 × 等级（不再乘箭矢 baseDamage）
        float extra = (float) villagers * level;
        if (extra <= 0 || Float.isNaN(extra) || Float.isInfinite(extra)) {
            return false;
        }
        // 每支箭对同一目标只算一次
        ResourceLocation key = rangedKey(target);
        if (persistentData.getBoolean(key)) {
            return false;
        }
        persistentData.putBoolean(key, true);

        DamageSource source = TinkerDamageTypes.source(
                projectile.level().registryAccess(), TinkerDamageTypes.MELEE_ARROW, arrow, attacker);
        ToolAttackUtil.attackEntitySecondary(source, extra, target, target, true);
        return false;
    }

    /** 去重键 = 固定标记 + 命中目标 id（穿射同一支箭打不同目标时各自结算一次） */
    private static ResourceLocation rangedKey(LivingEntity target) {
        return ResourceLocation.fromNamespaceAndPath("galtc2", "villager_metal_melee_ranged_done_" + target.getId());
    }

    /** 统计附近存活的村民(含流浪商人这类 AbstractVillager)，按需调用 */
    private static int countVillagers(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) {
            return 0;
        }
        AABB area = entity.getBoundingBox().inflate(RADIUS);
        int count = entity.level().getEntitiesOfClass(AbstractVillager.class, area, v -> v.isAlive() && !v.isRemoved()).size();
        return Math.min(count, MAX_VILLAGERS);
    }
}
