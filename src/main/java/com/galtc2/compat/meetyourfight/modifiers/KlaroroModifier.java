package com.galtc2.compat.meetyourfight.modifiers;

import com.galtc2.Galtc2;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 克拉罗罗 (klaroro) —— 迎战 联动强化：<b>击杀 boss 永久提升工具的全属性</b>。
 * <p>
 * 获取：工匠站(工具台)上用迎战 4 个 boss 的掉落物（幽魂质 / 命运之眷 / 藓牙 / 暮光紫罗兰）
 * 各 1 个安装，消耗 1 个能力槽（配方见 {@code data/galtc2/recipes/tool/modifiers/ability/klaroro.json}）。
 * <p>
 * 效果：<b>每击杀一个 boss，永久 +5% 全属性，上限 +1000%</b>（= 200 层）。
 * <ul>
 *   <li><b>只算 boss，不算小怪</b>：判定用 Forge 自带的实体类型标签 {@code forge:bosses}
 *       （{@link Tags.EntityTypes#BOSSES}）。原版只含末影龙/凋灵，迎战 自己把 4 个 boss
 *       都登记进去了，灾变/BOMD 之类也会登记，所以小怪天然不计入。
 *       另外并集自己的 {@code galtc2:bosses} 标签（{@link #EXTRA_BOSSES}），
 *       默认挂了 galboss 的三个 boss，整合包可以继续往里补。</li>
 *   <li>层数存在<b>这把工具自己的 persistentData</b> 里（和樱匠的soul_devourer 同一套写法），
 *       拆下强化/移除强化时会一起清掉。</li>
 *   <li>属性用 {@code AttributesModifierHook} 挂上去，只在"握着/穿着"这把工具时生效。</li>
 * </ul>
 * 注意：匠魂本体<b>没有击杀钩子</b>（`ModifierHooks` 里跟死/杀有关的只有 loot 系），
 * 所以这里自己在构造里挂 {@link LivingDeathEvent}（和 {@code KisakiModifier} 一样的做法）。
 */
public class KlaroroModifier extends Modifier implements AttributesModifierHook, TooltipModifierHook, ModifierRemovalHook {

    /** 击杀层数在工具持久化数据里的键 */
    private static final ResourceLocation KILLS = Galtc2.loc("klaroro_kills");
    /**
     * 额外的 boss 标签（{@code galtc2:bosses}，文件在
     * {@code data/galtc2/tags/entity_types/bosses.json}）。
     * 给"没把自己登记进 {@code forge:bosses} 的 boss"用：默认挂了 galboss 的三个 boss，
     * 整合包也可以往里面补或反过来排除某些 boss。
     */
    private static final TagKey<EntityType<?>> EXTRA_BOSSES =
            TagKey.create(Registries.ENTITY_TYPE, Galtc2.loc("bosses"));
    /** 每击杀一个 boss 的加成比例（5%） */
    private static final float PER_KILL = 0.05f;
    /** 加成上限（1000%） */
    private static final float MAX_BONUS = 10.0f;
    /** 层数上限 = 上限 / 每层 */
    private static final int MAX_KILLS = Math.round(MAX_BONUS / PER_KILL);

    /**
     * 默认"工具能获得的属性"清单：按 {@link AttributeModifier.Operation#MULTIPLY_TOTAL} 百分比叠加
     * （所以基础值为 0 的属性——击退抗性、幸运、攻击击退——加了也没效果，就不列了）。
     * 每个属性配一个固定 UUID，避免重复叠加。
     */
    private static final Bonus[] BONUSES = {
            new Bonus(Attributes.ATTACK_DAMAGE, UUID.fromString("4f1a2c83-6a7d-4b0e-9f21-3c5d7e9a1b02")),
            new Bonus(Attributes.ATTACK_SPEED, UUID.fromString("5b2c3d94-7b8e-4c1f-8a32-4d6e8f0b2c13")),
            new Bonus(Attributes.MAX_HEALTH, UUID.fromString("6c3d4ea5-8c9f-4d20-9b43-5e7f9a1c3d24")),
            new Bonus(Attributes.ARMOR, UUID.fromString("7d4e5fb6-9da0-4e31-ac54-6f8a0b2d4e35")),
            new Bonus(Attributes.ARMOR_TOUGHNESS, UUID.fromString("8e5f60c7-aeb1-4f42-bd65-7a9b1c3e5f46")),
            new Bonus(Attributes.MOVEMENT_SPEED, UUID.fromString("9f6071d8-bfc2-4053-ce76-8bac2d4f6057"))
    };

    private record Bonus(Attribute attribute, UUID uuid) {
    }

    public KlaroroModifier() {
        // 匠魂没有击杀钩子，自己接一个（死亡事件在客户端也会 fire，所以下面要判 isClientSide）
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDeath);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ATTRIBUTES, ModifierHooks.TOOLTIP, ModifierHooks.REMOVE);
    }

    /** 击杀判定：只认真 boss，并且只记在"击杀那一刻拿在手里"的那把工具上。 */
    private void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        EntityType<?> type = event.getEntity().getType();
        if (!type.is(Tags.EntityTypes.BOSSES) && !type.is(EXTRA_BOSSES)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity killer) || killer == event.getEntity()) {
            return;
        }
        ToolStack tool = heldTool(killer);
        if (tool == null || tool.isBroken()) {
            return;
        }
        int kills = tool.getPersistentData().getInt(KILLS);
        if (kills >= MAX_KILLS) {
            return;
        }
        tool.getPersistentData().putInt(KILLS, kills + 1);
    }

    /** 主手优先、其次副手；都不是带本强化的匠魂工具就返回 null。 */
    @Nullable
    private ToolStack heldTool(LivingEntity killer) {
        ToolStack main = ToolStack.from(killer.getMainHandItem());
        if (main.getModifierLevel(this) > 0) {
            return main;
        }
        ToolStack off = ToolStack.from(killer.getOffhandItem());
        return off.getModifierLevel(this) > 0 ? off : null;
    }

    /** 当前累计的加成比例（0.05 = +5%） */
    private static float bonusOf(IToolStackView tool) {
        int kills = tool.getPersistentData().getInt(KILLS);
        return Math.min(kills * PER_KILL, MAX_BONUS);
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry entry, EquipmentSlot slot,
                              BiConsumer<Attribute, AttributeModifier> consumer) {
        float bonus = bonusOf(tool);
        if (bonus <= 0) {
            return;
        }
        for (Bonus item : BONUSES) {
            consumer.accept(item.attribute(),
                    new AttributeModifier(item.uuid(), "galtc2:klaroro", bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry entry, @Nullable Player player, List<Component> tooltip,
                           TooltipKey key, TooltipFlag flag) {
        int kills = tool.getPersistentData().getInt(KILLS);
        tooltip.add(Component.translatable("modifier.galtc2.klaroro.current",
                String.format("%.0f", bonusOf(tool) * 100f), kills).withStyle(ChatFormatting.GOLD));
    }

    @Nullable
    @Override
    public Component onRemoved(IToolStackView tool, Modifier modifier) {
        // 强化被拆掉/洗掉时把层数一起清掉，免得留下"看不见的属性"
        tool.getPersistentData().remove(KILLS);
        return null;
    }
}
