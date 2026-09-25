package com.galtc2.content.effect;

import com.galtc2.Galtc2;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 樱纹病（{@code effect.galtc2.sakura}）。
 * <p>
 * 层数直接用原版效果的 amplifier 当计数器（{@code 层数 = amplifier + 1}，也就是效果等级），
 * 叠到 {@link #MAX_LAYERS} 层（V 级）后爆发致死。死因走自定义伤害类型
 * {@code galtc2:sakura_burst}，死亡消息 = {@code death.attack.sakura_burst}（"樱纹病爆发"）。
 * <p>
 * 全程只用原版 API，不依赖任何额外库：
 * <ul>
 *     <li>伤害类型定义：{@code data/galtc2/damage_type/sakura_burst.json}</li>
 *     <li>贯穿无敌帧与创造免伤：{@code data/minecraft/tags/damage_type/bypasses_invulnerability.json}</li>
 * </ul>
 * 效果本身由 {@code modifier.galtc2.oumunnbyou}（樱纹病）叠层，最多封顶 {@code MAX_LAYERS - 1} 级。
 */
public class sakura extends MobEffect {

    /** 爆发层数（= amplifier + 1，到 V 级就炸） */
    public static final int MAX_LAYERS = 5;

    /** 爆发用的伤害类型，对应上面的 damage_type json */
    public static final ResourceKey<DamageType> SAKURA_BURST = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Galtc2.MODID, "sakura_burst"));

    /** 身上飘樱瓣的间隔（tick） */
    private static final int PETAL_INTERVAL = 3;
    /** 爆发判定间隔（tick），被免死类效果挡住时不至于每 tick 重试 */
    private static final int BURST_INTERVAL = 10;

    public sakura() {
        super(MobEffectCategory.HARMFUL, 0xFFC0CB);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 只在服务端干活，粒子和音效都走服务端发包，附近玩家都能看到
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        int layers = Math.min(amplifier + 1, MAX_LAYERS);

        if (entity.tickCount % PETAL_INTERVAL == 0) {
            spawnPetals(level, entity, layers);
        }
        if (layers >= MAX_LAYERS && entity.tickCount % BURST_INTERVAL == 0) {
            burst(level, entity);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    /** 层数越高，身上飘的樱瓣越多 */
    private static void spawnPetals(ServerLevel level, LivingEntity entity, int layers) {
        double width = Math.max(entity.getBbWidth(), 0.4D);
        double height = Math.max(entity.getBbHeight(), 0.9D);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES,
                entity.getX(), entity.getY() + height * 0.6D, entity.getZ(),
                1 + layers * 2,
                width * 0.5D, height * 0.4D, width * 0.5D,
                0.01D);
    }

    /** 第 5 层：樱纹满身 → 炸成一圈花瓣，随即致死 */
    private void burst(ServerLevel level, LivingEntity entity) {
        double width = Math.max(entity.getBbWidth(), 0.4D);
        double height = Math.max(entity.getBbHeight(), 0.9D);

        level.sendParticles(ParticleTypes.CHERRY_LEAVES,
                entity.getX(), entity.getY() + height * 0.5D, entity.getZ(),
                60, width * 1.5D, height, width * 1.5D, 0.15D);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.CHERRY_LEAVES_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);

        Holder<DamageType> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(SAKURA_BURST);
        // 伤害拉满：护甲/抗性/无敌帧都拦不住，且死亡消息会显示"樱纹病爆发"
        entity.hurt(new DamageSource(type, null, null), Float.MAX_VALUE);
    }
}
