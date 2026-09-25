package com.galtc2.jvt.modifiers;

import com.galtc2.jvt.Jvt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

/**
 * 贵族强化(noble_upgrade)
 * <ul>
 *   <li>前置：部件中需含古二轮金属(validate)；</li>
 *   <li>护甲：由 NobleFactionEvents 通过 J村 阵营事件实现"不被复魂尸主动攻击"；</li>
 *   <li>近战：命中后召唤一只复魂尸(带冷却，避免刷屏)。</li>
 * </ul>
 */
public class NobleUpgradeModifier extends Modifier implements ValidateModifierHook, MeleeHitModifierHook {

    private static final MaterialId ANCIENT = new MaterialId(Jvt.MODID, "ancient_second_rounder");
    private static final ResourceLocation RESURRECTER = new ResourceLocation("jerotesvillage", "resurrecter");
    private static final ResourceLocation SUMMON_COOLDOWN = Jvt.loc("noble_summon_cooldown");
    /** 召唤冷却(刻)，默认 5 秒 */
    private static final int COOLDOWN_TICKS = 100;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.VALIDATE, ModifierHooks.MELEE_HIT);
    }

    @Override
    public Component validate(IToolStackView tool, ModifierEntry modifier) {
        for (MaterialVariant variant : tool.getMaterials().getList()) {
            if (variant.matches(ANCIENT)) {
                return null;
            }
        }
        return Component.translatable("modifier.jvt.noble_upgrade.requirements");
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (context.getLevel().isClientSide) {
            return;
        }

        // 冷却
        ModDataNBT data = tool.getPersistentData();
        int now = (int) (context.getLevel().getGameTime() & 0x7FFFFFFF);
        if (data.contains(SUMMON_COOLDOWN) && now - data.getInt(SUMMON_COOLDOWN) < COOLDOWN_TICKS) {
            return;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(RESURRECTER);
        if (type == null || !(context.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity entity = type.create(serverLevel);
        if (!(entity instanceof Mob mob)) {
            if (entity != null) {
                entity.discard();
            }
            return;
        }
        Vec3 pos = target.position();
        mob.moveTo(pos.x, pos.y, pos.z, serverLevel.random.nextFloat() * 360.0f, 0.0f);
        mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        serverLevel.addFreshEntity(mob);
        data.putInt(SUMMON_COOLDOWN, now);
    }
}
