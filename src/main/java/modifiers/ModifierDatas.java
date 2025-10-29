package modifiers;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;



public enum ModifierDatas {
    SPRB("sprb","add 2 upgrade","summon");
    public final String id;
    public final String flavor;
    public final String desc;

    ModifierDatas(String id, String flavor, String desc) {
        this.id = id;
        this.flavor = flavor;
        this.desc = desc;
    }

    public String asName() {
        return RegistrateLangProvider.toEnglishName(this.id);
    }

    public ModifierId getId() {
        return this.getId("tinkers_ingenuity");
    }

    public ModifierId getId(String modid) {
        return new ModifierId(modid, this.id);
    }

    public ModifierEntry asEntry(int lv) {
        return new ModifierEntry(this.getId(), lv);
    }

    public ModifierEntry asEntry() {
        return this.asEntry(1);
    }

    public String langKey() {
        return "modifier.tinkers_ingenuity." + this.id;
    }

    public String flavorKey() {
        return this.langKey() + ".flavor";
    }

    public String descKey() {
        return this.langKey() + ".description";
    }


}