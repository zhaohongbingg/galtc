package com.galtc2.compat.meetyourfight.modifiers;

import com.galtc2.Galtc2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.ForgeMod;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 魂喰 (soul_eater) —— 迎战(Meet Your Fight) 联动特性：幽魂质 材料。
 * <p>
 * 三条效果分别对应迎战 里用幽魂质 做的道具：
 * <ul>
 *   <li><b>+2 方块触及距离</b>（对应"幽灵之握"）：复用匠魂自带 {@code tconstruct:reach} 的做法，
 *       直接给 {@link ForgeMod#BLOCK_REACH} 加属性，握在手里/穿在身上都生效；</li>
 *   <li><b>近战对发光生物加伤</b>（对应"幽灵之眼"）：近战目标身上有 {@code minecraft:glowing}
 *       时 <b>+50%/级</b>，判定写在 {@code MELEE_DAMAGE} 钩子里（同匠魂的"导电"特性写法）；</li>
 *   <li><b>远程对发光生物加伤</b>（F-029 新增，F-031 调口径）：箭矢命中走 {@code PROJECTILE_HIT}，
 *       额外伤害 = 该箭 {@code baseDamage × 0.5 × 等级}，<b>与近战同口径（+50%/级）</b>。
 *       早先按用户原文做成固定 1.2 倍（= baseDamage × 0.2），但箭矢 baseDamage 本身就 2 点上下，
 *       实测只多 0.4 点，等于没有效果，故统一到近战公式；近战那一支仍是 {@code +50%/级}。</li>
 * </ul>
 * 远程用 {@code attackEntitySecondary} 补伤害（不碰 {@code target.hurt()}），并且
 * <b>每支箭只结算一次</b>（箭矢 persistentData 打 {@value #KEY_ARROW_DONE}），穿射不滚雪球。
 */
public class SoulEaterModifier extends Modifier
        implements AttributesModifierHook, MeleeDamageModifierHook, ProjectileHitModifierHook {

    /** 方块触及距离加成（格/级） */
    private static final int REACH_BONUS = 2;
    /** 对发光生物的额外伤害比例（0.5 = +50%/级），近战/远程共用同一口径 */
    private static final float GLOWING_DAMAGE_BONUS = 0.5f;
    /** 箭矢去重键：一支箭只结算一次（穿射不滚雪球）。写在 projectile 的 TConstruct 持久数据里 */
    private static final ResourceLocation KEY_ARROW_DONE = Galtc2.loc("soul_eater_done");
    /** 属性修饰符 UUID：固定值，避免同一件装备被重复叠 */
    private static final UUID REACH_UUID = UUID.fromString("8b1f4c52-5a2f-4d6e-9c3a-1d7f0b6e2a41");

    public SoulEaterModifier() {
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ATTRIBUTES, ModifierHooks.MELEE_DAMAGE, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot,
                              BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(ForgeMod.BLOCK_REACH.get(),
                new AttributeModifier(REACH_UUID, "galtc2:soul_eater_reach", REACH_BONUS * modifier.getEffectiveLevel(),
                        AttributeModifier.Operation.ADDITION));
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                float baseDamage, float damage) {
        // 只在服务端掷骰/判定，客户端预测走原伤害
        if (context.getAttacker().level().isClientSide()) {
            return damage;
        }
        LivingEntity target = context.getLivingTarget();
        if (target != null && target.hasEffect(MobEffects.GLOWING)) {
            return damage * (1f + GLOWING_DAMAGE_BONUS * modifier.getEffectiveLevel());
        }
        return damage;
    }

    /** 远程：对发光生物补一次 {@code baseDamage × 0.5 × 等级} 的额外伤害（与近战同口径） */
    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                         Projectile projectile, EntityHitResult hit,
                                         @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null || !target.isAlive() || !target.hasEffect(MobEffects.GLOWING)) {
            return false;
        }
        if (projectile.level().isClientSide() || !(projectile instanceof AbstractArrow arrow)) {
            return false;
        }
        // 注意 ModifierEntry#getEffectiveLevel() 返回 float，别用 int 接（会因精度丢失编不过）
        float level = modifier.getEffectiveLevel();
        if (level <= 0.0f) {
            return false;
        }
        // 每支箭只结算一次（倍率型必须去重）
        if (persistentData.getBoolean(KEY_ARROW_DONE)) {
            return false;
        }
        persistentData.putBoolean(KEY_ARROW_DONE, true);
        float baseDamage = (float) arrow.getBaseDamage();
        if (baseDamage <= 0.0f) {
            return false;
        }
        DamageSource source = attacker.damageSources().arrow(arrow, attacker);
        ToolAttackUtil.attackEntitySecondary(source, baseDamage * GLOWING_DAMAGE_BONUS * level,
                target, target, true);
        return false;
    }
}
