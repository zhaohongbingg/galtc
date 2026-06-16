package com.galtc2.content.items;

import com.galtc2.Register.galtcItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

public class GalRecordItem extends Item {

    private static final Random RANDOM = new Random();

    // 可掉落的材料物品列表，添加新材料时在这里加
    // 注意：gal_开头的物品和万花镜(kaleidoscope)是强化材料，不在此列
    private static final List<MaterialEntry> MATERIAL_POOL = List.of(
            new MaterialEntry(() -> galtcItem.sprb.get(), "sprb"),
            new MaterialEntry(() -> galtcItem.zeroone.get(), "zeroone"),
            new MaterialEntry(() -> galtcItem.kagome.get(), "kagome"),
            new MaterialEntry(() -> galtcItem.aosora.get(), "aosora"),
            new MaterialEntry(() -> galtcItem.kagari.get(), "kagari"),
            new MaterialEntry(() -> galtcItem.atri.get(), "atri"),
            new MaterialEntry(() -> galtcItem.apeiria.get(), "apeiria"),
            new MaterialEntry(() -> galtcItem.einstein.get(), "einstein"),
            new MaterialEntry(() -> galtcItem.narcissus.get(), "narcissus"),
            new MaterialEntry(() -> galtcItem.ca.get(), "ca"),
            new MaterialEntry(() -> galtcItem.steinsgate.get(), "steinsgate"),
            new MaterialEntry(() -> galtcItem.azarashi.get(), "azarashi"),
            new MaterialEntry(() -> galtcItem.nighttimesheep.get(), "nighttimesheep"),
            new MaterialEntry(() -> galtcItem.oumunnbyou.get(), "oumunnbyou"),
            new MaterialEntry(() -> galtcItem.kisaragikurenai.get(), "kisaragikurenai"),
            new MaterialEntry(() -> galtcItem.blacklily.get(), "blacklily"),
            new MaterialEntry(() -> galtcItem.muramasa.get(), "muramasa"),
            new MaterialEntry(() -> galtcItem.shochou.get(), "shochou"),
            new MaterialEntry(() -> galtcItem.izumi_hiyori.get(), "izumi_hiyori")
    );

    public GalRecordItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // 随机选一个材料
            MaterialEntry entry = MATERIAL_POOL.get(RANDOM.nextInt(MATERIAL_POOL.size()));

            // 掉落1-2个
            int count = 1 + RANDOM.nextInt(2);
            ItemStack dropStack = new ItemStack(entry.item().get(), count);

            // 在玩家位置掉落
            player.drop(dropStack, false);

            // 消耗一个 gal_record（如果不是创造模式）
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            // 统计
            player.awardStat(Stats.ITEM_USED.get(this));

            // 音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.0F);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    // 材料条目，方便扩展
    public record MaterialEntry(java.util.function.Supplier<Item> item, String name) {}
}
