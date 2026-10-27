package com.galtc2.modifiers;

import com.galtc2.compat.aetherdragon.AetherDragonCompat;
import com.galtc2.compat.aetherdragon.AetherDragonEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Set;

/**
 * 丸子与银河龙 (shaday) —— 阴影材料的工具侧特性（龙系，材料来自天境龙的阴影套）。
 * <p>
 * <b>攻击特效</b>：命中时调用天境巨龙自己的特效库，在目标身上炸一次龙的爪击/羽刃冲击
 * （{@code DragonSkillEntity} 事件 1 + {@code dragon_claw_hit} 音效），见
 * {@link AetherDragonEffects}。
 * <p>
 * <b>额外伤害</b>：对<b>非天境生物</b>造成 {@value #NON_AETHER_BONUS} × 额外伤害（即最终伤害 ×1.5）。
 * 「天境生物」按命名空间认：{@code aether}（天境本体）、{@code aether_bedrock_mobs_reborn}、
 * {@code aether_dragon} 的生物视为天境生物；其余（原版、其它模组）都吃额外伤害。
 * <p>
 * 没装天境巨龙时（理论上阴影材料也不会出现，因为 definition 有 mod_loaded 条件）：
 * 额外伤害照常生效，只是不播龙特效 —— 特效代码在 isLoaded 判断之后才会被类加载。
 */
public class ShadayModifier extends BaseModifiers {

    /** 对非天境生物的额外伤害比例 */
    private static final float NON_AETHER_BONUS = 0.5F;

    /** 「天境系」命名空间：它们的生物不吃额外伤害 */
    private static final Set<String> AETHER_NAMESPACES = Set.of(
            "aether",
            "aether_bedrock_mobs_reborn",
            "aether_dragon"
    );

    public ShadayModifier() {
    }

    @Override
    public float onModifyMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                     LivingEntity attacker, LivingEntity target, float baseDamage, float actualDamage) {
        if (isAetherCreature(target)) {
            return actualDamage;
        }
        return actualDamage * (1.0F + NON_AETHER_BONUS);
    }

    @Override
    public void onAfterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                LivingEntity attacker, LivingEntity target, float damageDealt) {
        if (damageDealt <= 0.0F || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        // 只有装了天境巨龙才会加载 AetherDragonEffects（那里才引用它的粒子/音效表）
        if (AetherDragonCompat.isLoaded()) {
            AetherDragonEffects.attackFx(level, attacker, target);
        }
    }

    /** 目标是不是「天境系」生物（按实体类型的命名空间判断） */
    private static boolean isAetherCreature(LivingEntity target) {
        return AETHER_NAMESPACES.contains(EntityType.getKey(target.getType()).getNamespace());
    }
}
