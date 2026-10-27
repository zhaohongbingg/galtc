package com.galtc2.jvt.modifiers;

import com.galtc2.Galtc2;
import com.jerotes.jerotes.forge.JerotesMerorDamageEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.Optional;

/**
 * 莫厄箭(meror_bolt) —— 莫厄 / 精炼莫厄 材料的远程特性：<b>附加莫厄伤害</b>。
 * <p>
 * 为什么必须单开一个类（对齐 {@code docs/analysis/F-029-ranged-mapping.md} §4-2/§4-3）：
 * J村 判"莫厄伤害"靠 {@code EntityAndItemFind.isMerorAttack(source)} → post
 * {@link JerotesMerorDamageEvent}，而它自己只认两种情况：① 直接/间接实体属于
 * {@code jerotes:meror_damage} 实体标签；② 近战且攻击者主手在 {@code jerotes:meror} 物品标签里。
 * 玩家用弓射出的箭两种情况都不满足 → 永远是"普通伤害"：
 * <ul>
 *   <li>不会吃到莫厄增伤/减伤的语义；</li>
 *   <li>反过来会被 {@code MerorResistanceModifier} 当成非莫厄伤害**减伤 12%/级**（打自己人）。</li>
 * </ul>
 * 所以这里改用自有伤害类型 {@code galtc2:meror_shot}，并在 {@link MerorEvents} 里
 * 让 J村 认账（把事件的 {@code merorDamage} 置真），语义与 {@code MerorResistanceModifier} 自洽。
 * <p>
 * 数值：额外伤害 = {@code arrow.getBaseDamage() × }{@value #EXTRA_DAMAGE_PER_LEVEL}
 * {@code × 等级}（1 级 = 追加 50%，2 级 = 追加 100%）。伤害一律走
 * {@link ToolAttackUtil#attackEntitySecondary}（会临时清空并还原 {@code invulnerableTime}），
 * <b>严禁在 PROJECTILE_HIT 里 {@code target.hurt()}</b>，否则会把箭矢自身的伤害吃掉。
 * <p>
 * 软联动：事件只在 {@link #registerEvents()} 里、确认 J村 已加载后才挂；
 * 未装 J村 时不注册，{@code galtc2:meror_shot} 按普通伤害结算（本类也不会被加载，见 {@code Jvt.init} 的门槛）。
 * 伤害类型数据缺失时退回匠魂通用来源，绝不抛异常。
 * <p>
 * 去重：每支箭只结算一次（箭矢 persistentData 标记 {@link #DONE}）。
 * 等级：<b>按等级缩放</b>（不重写任何"无等级"标记，{@code modifier.getLevel()} 直接参与计算）。
 */
public class MerorRangedModifier extends Modifier implements ProjectileHitModifierHook {

    /** 莫厄伤害：额外伤害 = 该箭 baseDamage × 本系数 × 等级（1 级追加 50%，2 级追加 100%） */
    public static final float EXTRA_DAMAGE_PER_LEVEL = 0.5f;

    /** 自有伤害类型 galtc2:meror_shot（数据侧由 b-data 提供） */
    public static final ResourceKey<DamageType> MEROR_SHOT =
            ResourceKey.create(Registries.DAMAGE_TYPE, Galtc2.loc("meror_shot"));

    /** 每支箭只结算一次的标记（字面即 {@code galtc2_meror_bolt_done}） */
    public static final ResourceLocation DONE = Galtc2.loc("meror_bolt_done");

    /** J村 modid（软联动判断用） */
    private static final String JEROTES_MOD_ID = "jerotes";

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_HIT);
    }

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
        float extra = (float) arrow.getBaseDamage() * EXTRA_DAMAGE_PER_LEVEL * level;
        if (extra <= 0 || Float.isNaN(extra) || Float.isInfinite(extra)) {
            return false;
        }
        // 每支箭只结算一次（穿射也不叠加）
        if (persistentData.getBoolean(DONE)) {
            return false;
        }
        persistentData.putBoolean(DONE, true);

        ToolAttackUtil.attackEntitySecondary(merorSource(projectile, attacker), extra, target, target, true);
        return false;
    }

    /**
     * 取 {@code galtc2:meror_shot} 伤害源；该伤害类型未注册（数据包缺失）时退回
     * 匠魂通用来源 {@code tconstruct:melee_arrow}，保证"打得出伤害、不抛异常"。
     */
    private static DamageSource merorSource(Projectile projectile, LivingEntity attacker) {
        Level level = projectile.level();
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Optional<Holder.Reference<DamageType>> holder = registry.getHolder(MEROR_SHOT);
        if (holder.isPresent()) {
            return new DamageSource(holder.get(), projectile, attacker);
        }
        return TinkerDamageTypes.source(level.registryAccess(), TinkerDamageTypes.MELEE_ARROW, projectile, attacker);
    }

    /**
     * 让 J村 把 {@code galtc2:meror_shot} 认成"莫厄伤害"。
     * <p>
     * J村 的判定入口是 {@code EntityAndItemFind.isMerorAttack(DamageSource)}，它 post 一个
     * {@link JerotesMerorDamageEvent} 并读 {@code isMerorDamage()}；我们在这里对自家伤害类型置真，
     * 于是 {@code MerorResistanceModifier}（莫厄伤害不减伤）等 J村/本模组逻辑都能一致工作。
     * <p>
     * 只由 {@link #registerEvents()} 在确认 J村 已加载后挂到 Forge 事件总线，
     * 不用 {@code @Mod.EventBusSubscriber}（否则未装 J村 时也会被 Forge 加载本类）。
     */
    public static final class MerorEvents {

        private MerorEvents() {
        }

        @SubscribeEvent
        public static void onMerorDamageCheck(JerotesMerorDamageEvent event) {
            if (event.isMerorDamage()) {
                return;
            }
            DamageSource source = event.getDamageSource();
            if (source != null && source.is(MEROR_SHOT)) {
                event.setMerorDamage(true);
            }
        }
    }

    /**
     * 注册 {@link MerorEvents}（由 {@code Jvt.init} 在"J村 已加载"的分支里调用）。
     * 这里再判一次 {@link ModList}，保证任何调用路径下未装 J村 都不会挂上依赖 J村 事件类的监听器。
     */
    public static void registerEvents() {
        if (!ModList.get().isLoaded(JEROTES_MOD_ID)) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(MerorEvents.class);
    }
}
