package com.galtc2.compat.aetherdragon;

import net.minecraftforge.fml.ModList;

/**
 * 天境巨龙(Aether Dragon) 软联动入口 —— 只有「阴影」材料的龙系特性才会用到。
 * <p>
 * 这个类<b>刻意不 import 任何 aether_dragon 的类</b>：主模组的特性实现（{@code ShadayModifier} 等）
 * 会先调这里的 {@link #isLoaded()}，只有装了模组时才去碰
 * {@link AetherDragonEffects}（那里才引用 {@code DragonSkillEntity} / {@code ModSounds}），
 * 这样没装天境巨龙时阴影材料最多只是没有龙特效，不会 NoClassDefFoundError。
 * <p>
 * 模组信息：CurseForge 项目 1740349 / 文件 9306964 = aether_dragon-1.4.1.jar，modid {@value #MOD_ID}；
 * 阴影套（{@code shaday_*}）与天境龙都是它注册的，也是「天境基石生物重制」的强制前置。
 */
public final class AetherDragonCompat {

    /** 天境巨龙 modid */
    public static final String MOD_ID = "aether_dragon";

    private AetherDragonCompat() {
    }

    /** 运行环境是否装了天境巨龙 */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }
}
