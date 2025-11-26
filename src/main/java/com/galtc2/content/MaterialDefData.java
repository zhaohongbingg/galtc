package com.galtc2.content;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.*;
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

        public Builder tagCondition(ResourceLocation condition) {
            return orCondition(new NotCondition(new TagEmptyCondition(condition)));
        }

        public Builder modCondition(String condition) {
            return orCondition(new ModLoadedCondition(condition));
        }

        public MaterialDefData build() {
            return new MaterialDefData(this.tier, this.order, this.craftable, this.hidden, this.condition);
        }
    }

}
