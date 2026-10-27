package com.galtc2.modifiers;

import com.galtc2.Galtc2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.UUID;
import java.util.function.BiConsumer;

public class KisaragiKurenai extends BaseModifiers{
    /**
     * 「这一发箭已经吃过绯红加成」的一次性标记。
     * <p>
     * 存在箭矢实体的 TConstruct PersistentData capability 上（{@code ToolEvents#projectileHit} 里
     * {@code PersistentDataCapability.getOrWarn(projectile)}，随箭矢存活、穿射期间持续），
     * 也就是本类 {@code onProjectileHitTarget} 的第一个 {@link ModDataNBT} 参数。
     * <p>
     * 为什么需要它：{@code PROJECTILE_HIT} 在 {@code ProjectileImpactEvent} 里派发，
     * <b>穿射（piercing）箭每命中一个实体都会再派发一次</b>，而
     * {@code arrow.setBaseDamage(arrow.getBaseDamage() * factor)} 会把倍率写回箭矢本体，
     * 于是第 n 个目标吃到的倍率是 (1+0.25L)^n —— L3 穿 4 只约 9.4 倍，指数级叠加。
     * 打上标记后只在首次命中乘一次；倍率本身留在箭矢上，所以后续目标与第一个目标伤害一致（不再递增）。
     */
    private static final ResourceLocation PIERCE_BOOSTED = Galtc2.loc("kk_pierce_boosted");

    public KisaragiKurenai() {

    }
    @Override
    public boolean isNoLevels() {
        return false;
    }


    @Override
    public float getMeleeDamage(IToolStackView tool, slimeknights.tconstruct.library.modifiers.ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity enemy=context.getLivingTarget();
        double factor=1+modifier.getLevel()*0.25;
        if(!isSpider(enemy)){
            return (float) (damage*factor);
        }
        return damage;
    }

    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level, Projectile projectile, AbstractArrow arrow, EntityHitResult hit, LivingEntity attacker, LivingEntity target) {
        // 蜘蛛不享受加成，也不消耗「一次性」标记：后面的普通目标仍然该吃到这一次加成
        if (isSpider(target) || persistentData.contains(PIERCE_BOOSTED)) {
            return;
        }
        double factor=1+level*0.25;
        arrow.setBaseDamage(arrow.getBaseDamage()*factor);
        // 只打标记，不回滚：倍率留在箭矢上，穿射的第 2..n 个目标伤害与第一个目标相同，不再继续乘
        persistentData.putBoolean(PIERCE_BOOSTED, true);
    }

    private static boolean isSpider(LivingEntity entity){
        return entity instanceof Spider||
                entity instanceof CaveSpider;

    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Attribute, AttributeModifier> consumer) {
         double extraHealth=5+modifier.getLevel();
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            consumer.accept(
                    Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            UUID.fromString("c9c9c9c9-c9c9-c9c9-c9c9-c9c9c9c9c9c9"),
                            "tconstruct.kisaragi_kurenai.health",
                            extraHealth,
                            AttributeModifier.Operation.ADDITION
                    )
            );
        }

    }
}
