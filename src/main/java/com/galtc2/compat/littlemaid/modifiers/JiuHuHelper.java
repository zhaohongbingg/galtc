package com.galtc2.compat.littlemaid.modifiers;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.item.AbstractStoreMaidItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 「酒狐」相关的共用判定 —— 三个词条（日久生情 / 挚爱 / 同心）都要用。
 * <p>
 * <b>酒狐是谁</b>：玩家驯服的车万女仆 女仆（{@link EntityMaid}）。整合包里她用的是
 * {@code wine_fox}（Wine Fox / 酒狐）那套模型，所以本类在「多只女仆」时<b>优先</b>挑
 * 名牌或模型 ID 能对上「酒狐 / wine_fox」的那一只；一只都对不上（比如玩家自己改了名字）
 * 就退回到离玩家最近的一只自家女仆 —— 否则词条会因为「认不出哪只是酒狐」直接失效。
 * <p>
 * <b>好感度</b>：车万女仆 1.5.x 的 {@link EntityMaid#getFavorability()} 返回的是<b>点数</b>
 * （0 ~ {@value #MAX_FAVORABILITY}，64 / 192 / 384 是模组自己的 3 个等级门槛）。
 * 词条要求 6 档 1/2/2/3/3/4 点，所以这里按<b>每 {@value #POINTS_PER_BAND} 点一档</b>切成 6 档，
 * 取满 6 档后不再增加（也就是好感度点满时给最高的 4 点）。
 * <p>
 * 本类引用车万女仆 的类型，只能在确认车万女仆 已安装之后加载。
 */
public final class JiuHuHelper {

    /** 好感度点数上限（车万女仆 1.5.x：0 ~ 384 点，384 为满好感） */
    public static final int MAX_FAVORABILITY = 384;

    /** 每档需要的好感度点数 */
    public static final int POINTS_PER_BAND = 64;

    /** 4 档加成：好感度越高，词条补的基础面板越多 */
    private static final int[] BAND_BONUS = {1, 2, 3, 4};

    /** 找酒狐 / 找索敌目标的搜索半径（格） */
    public static final double SEARCH_RANGE = 32.0D;

    /** 名牌里出现这些字样就当她是酒狐 */
    private static final String[] NAME_HINTS = {"酒狐", "wine fox", "winefox"};

    /** 模型 ID（车万女仆 自己的模型 / YSM 模型）里出现这些字样就当她是酒狐 */
    private static final String[] MODEL_HINTS = {"wine_fox", "winefox", "jiuhu", "jiu_hu"};

    /** 原版/模组通用的 boss 实体标签（祸乱鬼魅 的 boss 也打在这个标签上） */
    private static final TagKey<EntityType<?>> BOSSES =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "bosses"));

    private JiuHuHelper() {
    }

    /* ------------------------------------------------------------ 是谁 */

    /** 是不是「酒狐」：被玩家驯服、还活着的车万女仆 女仆。 */
    public static boolean isJiuHu(@Nullable LivingEntity entity) {
        return entity instanceof EntityMaid maid && maid.isAlive() && maid.getOwner() instanceof Player;
    }

    /**
     * 找这名玩家身边的酒狐：优先名牌 / 模型能对上的那一只，其次最近的自家女仆。
     * 服务端 / 客户端都能调用（客户端拿到的是同步过来的女仆数据）。
     *
     * @param owner 工具 / 盔甲的使用者（一般就是玩家）
     * @return 找不到时返回 null
     */
    @Nullable
    public static EntityMaid findJiuHu(@Nullable LivingEntity owner) {
        if (!(owner instanceof Player player)) {
            return null;
        }
        List<EntityMaid> maids = player.level().getEntitiesOfClass(EntityMaid.class,
                player.getBoundingBox().inflate(SEARCH_RANGE),
                maid -> maid.isAlive() && player.equals(maid.getOwner()));
        if (maids.isEmpty()) {
            return null;
        }
        EntityMaid matched = null;
        double matchedDistance = Double.MAX_VALUE;
        EntityMaid nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (EntityMaid maid : maids) {
            double distance = maid.distanceToSqr(player);
            if (distance < matchedDistance && looksLikeJiuHu(maid)) {
                matched = maid;
                matchedDistance = distance;
            }
            if (distance < nearestDistance) {
                nearest = maid;
                nearestDistance = distance;
            }
        }
        // 多只酒狐时按「好感度最高」的那只算，避免加成在两三只之间跳动、最后锁到最低的一只
        EntityMaid best = bestJiuHu(player);
        if (best != null) {
            return best;
        }
        return matched != null ? matched : nearest;
    }

    /** 名牌或模型 ID 像不像酒狐 */
    private static boolean looksLikeJiuHu(EntityMaid maid) {
        if (maid.hasCustomName()) {
            String name = maid.getCustomName() == null ? "" : maid.getCustomName().getString().toLowerCase();
            for (String hint : NAME_HINTS) {
                if (name.contains(hint)) {
                    return true;
                }
            }
        }
        return containsHint(maid.getModelId()) || containsHint(maid.getYsmModelId());
    }

    private static boolean containsHint(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        String lower = id.toLowerCase();
        for (String hint : MODEL_HINTS) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    /* ------------------------------------------------------------ 好感度 */

    /** 酒狐当前的原始好感度点数（0 ~ {@value #MAX_FAVORABILITY}） */
    public static int favorability(@Nullable EntityMaid maid) {
        return maid == null ? 0 : Mth.clamp(maid.getFavorability(), 0, MAX_FAVORABILITY);
    }

    /**
     * 好感度换算出来的基础面板加成（1 / 2 / 3 / 4，门槛 0 / 64 / 128 / 192）。
     * <p>
     * 没有酒狐时按 0 点算，也就是最低档的 1 点 —— 词条本身至少要有存在感。
     */
    /**
     * 玩家身边所有酒狐里<b>好感度最高</b>的那只（多只酒狐时加成不会来回跳、也不会锁到最低那只）。
     */
    @Nullable
    public static EntityMaid bestJiuHu(@Nullable LivingEntity owner) {
        if (!(owner instanceof Player player)) {
            return null;
        }
        List<EntityMaid> maids = player.level().getEntitiesOfClass(EntityMaid.class,
                player.getBoundingBox().inflate(SEARCH_RANGE),
                maid -> maid.isAlive() && player.equals(maid.getOwner()));
        EntityMaid best = null;
        int bestFav = -1;
        for (EntityMaid maid : maids) {
            int fav = favorability(maid);
            if (fav > bestFav) {
                best = maid;
                bestFav = fav;
            }
        }
        return best;
    }

    /**
     * 魂符（{@code touhou_little_maid:smart_slab_*}）里保存着的女仆的好感度。
     * <p>
     * 女仆被收入魂符后就不是世界里的实体了，按实体找不到她；但魂符物品的 NBT 里存了
     * 完整女仆数据（{@code AbstractStoreMaidItem#getMaidData}，键 {@code MaidInfo}），
     * 其中 {@code MaidFavorability} 就是好感度点数。这里扫描玩家背包/副手，取最高的那一个。
     */
    public static int storedFavorability(@Nullable Player player) {
        if (player == null) {
            return 0;
        }
        int best = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            best = Math.max(best, storedFavorability(inventory.getItem(i)));
        }
        best = Math.max(best, storedFavorability(player.getOffhandItem()));
        return best;
    }

    private static int storedFavorability(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbstractStoreMaidItem)) {
            return 0;
        }
        if (!AbstractStoreMaidItem.hasMaidData(stack)) {
            return 0;
        }
        return Mth.clamp(AbstractStoreMaidItem.getMaidData(stack).getInt("MaidFavorability"),
                0, MAX_FAVORABILITY);
    }

    /** 由好感度点数直接换算档位加成（1 / 2 / 3 / 4） */
    public static int bonusForFavorability(int favorability) {
        int band = Mth.clamp(favorability / POINTS_PER_BAND, 0, BAND_BONUS.length - 1);
        return BAND_BONUS[band];
    }

    /**
     * 工具/护甲持有者能吃到的好感度加成：在场酒狐和魂符里保存的酒狐，取<b>好感度最高</b>的那只。
     * 这样女仆被收进魂符时，词条不会突然掉回 +1。
     */
    public static int affectionBonusFor(@Nullable LivingEntity owner) {
        if (!(owner instanceof Player player)) {
            return BAND_BONUS[0];
        }
        int favorability = 0;
        EntityMaid best = bestJiuHu(player);
        if (best != null) {
            favorability = favorability(best);
        }
        favorability = Math.max(favorability, storedFavorability(player));
        return bonusForFavorability(favorability);
    }

    public static int affectionBonus(@Nullable EntityMaid maid) {
        int band = Mth.clamp(favorability(maid) / POINTS_PER_BAND, 0, BAND_BONUS.length - 1);
        return BAND_BONUS[band];
    }

    /* ------------------------------------------------------------ boss 判定 */

    /** 是不是 boss（forge:bosses 标签，boss 类模组的 boss 都会打进来） */
    public static boolean isBoss(@Nullable LivingEntity entity) {
        return entity != null && entity.getType().is(BOSSES);
    }
}
