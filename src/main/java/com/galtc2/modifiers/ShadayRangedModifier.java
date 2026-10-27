package com.galtc2.modifiers;

import com.galtc2.compat.aetherdragon.AetherDragonCompat;
import com.galtc2.compat.aetherdragon.AetherDragonEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

/**
 * 龙姬混～日子 (shaday_ranged) —— 阴影材料的远程特性（龙系）。
 * <p>
 * 弹射物命中目标后，在命中点周围的 <b>5×5</b> 范围内依次降下 <b>5 根龙刺</b>：
 * 每根都是天境巨龙自己的 {@code DragonSkillEntity.SPIKE}（大号滴水石锥造型，
 * 落地前有收缩预警环，顶起时造成范围伤害与挑飞，并播放 {@code dragon_spike_erupt}）。
 * 调用集中在 {@link AetherDragonEffects#spawnSpikes}，没装天境巨龙时静默跳过。
 */
public class ShadayRangedModifier extends BaseModifiers {

    public ShadayRangedModifier() {
    }

    @Override
    public void onProjectileHitTarget(ModifierNBT modifiers, ModDataNBT persistentData, int level,
                                      Projectile projectile, AbstractArrow arrow, EntityHitResult hit,
                                      LivingEntity attacker, LivingEntity target) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!AetherDragonCompat.isLoaded()) {
            return;
        }
        AetherDragonEffects.spawnSpikes(serverLevel, attacker, hit.getLocation());
    }
}
