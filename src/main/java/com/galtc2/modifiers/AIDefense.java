package com.galtc2.modifiers;

import com.galtc2.Galtc2;
import com.galtc2.GaltcConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.List;

public class AIDefense extends BaseModifiers{
    public static final ResourceLocation KEY = Galtc2.loc("ai_defense");

    private static final double IMMUNE_TICKS = GaltcConfig.COMMON.IMMUNE_TICKS.get();  // 30秒



    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry entry,
                                   EquipmentContext ctx, EquipmentSlot slot,
                                   DamageSource source, float amount, boolean isDirect) {

        CompoundTag tag = tool.getPersistentData().getCompound(KEY);

        String lastId = tag.getString("id");
        long lastTime = tag.getLong("time");

        String id = source.getMsgId();
        long gameTime = ctx.getEntity().level().getGameTime();

        // ✔ 在免疫时间内且伤害类型相同 → 免疫
        if (lastId.equals(id) && gameTime - lastTime <= IMMUNE_TICKS) {
            return 0f;
        }

        // ✔ 记录新的伤害类型与时间
        tag.putString("id", id);
        tag.putLong("time", gameTime);
        tool.getPersistentData().put(KEY, tag);

        return amount;
    }
    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier,
                           @Nullable Player player, List<Component> tooltip,
                           TooltipKey key, TooltipFlag flag) {
        if (player == null) return;

        CompoundTag tag = tool.getPersistentData().getCompound(KEY);
        String lastId = tag.getString("id");
        long lastTime = tag.getLong("time");
        long gameTime = player.level().getGameTime();

        if (!lastId.isEmpty() && gameTime - lastTime <= IMMUNE_TICKS) {
            double remainingTicks = IMMUNE_TICKS - (gameTime - lastTime);
            double seconds = remainingTicks / 20;

            tooltip.add(Component.literal("免疫伤害: " + lastId + " 剩余 " + seconds + " 秒")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

}
