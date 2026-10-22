package com.galtc2.content;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.common.crafting.conditions.*;
import slimeknights.mantle.recipe.condition.TagFilledCondition;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.library.materials.definition.Material;

import javax.annotation.Nullable;

public record MaterialDefData(int tier, int order, boolean craftable, boolean hidden, @Nullable ICondition condition)
{
    public Material getMate(ResourceLocation id){
        return  new Material(id,tier,order,craftable,hidden);
    }
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int tier = 4;
        private int order = 2;
        private boolean craftable;
        private boolean hidden = false;
        private ICondition condition = null;

        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder craftable() {
            this.craftable = true;
            return this;
        }

        public Builder hidden() {
            this.hidden = true;
            return this;
        }

        public Builder orCondition(ICondition condition) {
            this.condition = new OrCondition(ConfigEnabledCondition.FORCE_INTEGRATION_MATERIALS, condition);
            return this;
        }

        /**
         * 联动材料：只有「物品标签被填充」时才加载（或玩家在配置里强制开启联动材料）。
         * <p>
         * 用的是 Mantle 的 {@code mantle:tag_filled}，和匠魂自家联动材料
         * （{@code data/tconstruct/tinkering/materials/definition/aluminum.json} 等）写法一致。
         * <b>不要</b>再写成 {@code forge:or} + {@code forge:not} + {@code forge:tag_empty}：
         * 那个组合虽然逻辑等价（{@code not(tag_empty) == tag_filled}），但不是匠魂/Mantle 的规范写法，
         * 而且 {@code forge:tag_empty} 只能查物品注册表，Mantle 版本还能指定 {@code registry}。
         *
         * @param tag 物品标签，一般形如 {@code forge:ingots/<材料名>}
         */
        public Builder tagCondition(ResourceLocation tag) {
            return orCondition(new TagFilledCondition<>(TagKey.create(Registries.ITEM, tag)));
        }

        /**
         * 联动金属材料的快捷写法：标签固定为 {@code forge:ingots/<materialId>}。
         * <p>
         * 注意标签必须由「持有该锭的模组」提供：J村 的锭标签就是
         * {@code forge:ingots/{meror_metal, qoaiku_metal, refine_meror_metal, serpon_alloy, villager_metal}}；
         * 而 galtc2 自己用 {@code registerMetal} 注册的锭（{@code galtc2:<名>_ingot}）目前
         * <b>没有</b>生成 {@code forge:ingots/<名>} 标签，所以本模组自己的材料不要用 tag 条件，
         * 请改用 {@link #modCondition(String)}。
         */
        public Builder metalTagCondition(String materialId) {
            return tagCondition(ResourceLocation.tryParse("forge:ingots/" + materialId));
        }

        public Builder modCondition(String condition) {
            return orCondition(new ModLoadedCondition(condition));
        }

        public MaterialDefData build() {
            return new MaterialDefData(this.tier, this.order, this.craftable, this.hidden, this.condition);
        }
    }

}
