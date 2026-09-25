package com.galtc2.jvt.Register;

import com.galtc2.jvt.Jvt;
import com.galtc2.jvt.modifiers.AncientSecondRounderModifier;
import com.galtc2.jvt.modifiers.BigWitchHatModifier;
import com.galtc2.jvt.modifiers.BitterColdHammerModifier;
import com.galtc2.jvt.modifiers.BrightLandBeastHornModifier;
import com.galtc2.jvt.modifiers.BrightLandHeartModifier;
import com.galtc2.jvt.modifiers.ChainBindModifier;
import com.galtc2.jvt.modifiers.ChainsawModifier;
import com.galtc2.jvt.modifiers.CrazyAxeModifier;
import com.galtc2.jvt.modifiers.DroughtWildfireShieldModifier;
import com.galtc2.jvt.modifiers.FirepowerModifier;
import com.galtc2.jvt.modifiers.GiantMonsterHairArmorModifier;
import com.galtc2.jvt.modifiers.GiantMonsterHairMeleeModifier;
import com.galtc2.jvt.modifiers.HagEyeModifier;
import com.galtc2.jvt.modifiers.MagickaScaleUpgradeModifier;
import com.galtc2.jvt.modifiers.MerorResistanceModifier;
import com.galtc2.jvt.modifiers.MerorStandardModifier;
import com.galtc2.jvt.modifiers.MountainRealmModifier;
import com.galtc2.jvt.modifiers.NobleUpgradeModifier;
import com.galtc2.jvt.modifiers.PurpleSandHagEyeModifier;
import com.galtc2.jvt.modifiers.QoaikuMetalModifier;
import com.galtc2.jvt.modifiers.ScalyBearScaleModifier;
import com.galtc2.jvt.modifiers.SeawaveTridentModifier;
import com.galtc2.jvt.modifiers.SerponAlloyModifier;
import com.galtc2.jvt.modifiers.SerponPowerhouseModifier;
import com.galtc2.jvt.modifiers.ShieldBreakModifier;
import com.galtc2.jvt.modifiers.TestModifier;
import com.galtc2.jvt.modifiers.VillagerMetalModifier;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

/** 修饰器注册位。scroll_cast = 卷轴·法术刻印(联动 Jerotes仓库 魔法体系)。 */
public class JvtModifier {
    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Jvt.MODID);

    /** 贵族强化的 ID，供 {@code NobleFactionEvents} 的阵营判定使用。 */
    public static final ModifierId NOBLE_UPGRADE = new ModifierId(Jvt.MODID, "noble_upgrade");

    /** 四个武器能力强化的 ID，用于互斥判定。 */
    public static final ModifierId SHIELD_BREAK = new ModifierId(Jvt.MODID, "shield_break");
    public static final ModifierId CHAINSAW = new ModifierId(Jvt.MODID, "chainsaw");
    public static final ModifierId FIREPOWER = new ModifierId(Jvt.MODID, "firepower");
    public static final ModifierId CRAZY_AXE = new ModifierId(Jvt.MODID, "crazy_axe");
    public static final ModifierId BITTER_COLD_HAMMER = new ModifierId(Jvt.MODID, "bitter_cold_hammer");
    public static final ModifierId SEAWAVE_TRIDENT = new ModifierId(Jvt.MODID, "seawave_trident");
    public static final ModifierId DROUGHT_WILDFIRE_SHIELD = new ModifierId(Jvt.MODID, "drought_wildfire_shield");

    // 注：scroll_cast / fire_apostle 由 galtc2 的 compat.jerotes.JerotesModifiers 注册
    //（那份是 jvt 实现的 galtc2 适配版，且只在装了 J村 时加载），这里不再重复注册。
    /** 占位测试特性(材料通用，后续替换)。 */
    public static final StaticModifier<TestModifier> test =
            MODIFIERS.register("test", TestModifier::new);

    /** 莫厄减伤(普通莫厄=莫厄金属套特性): 非莫厄伤害减伤, 穿戴件数叠加等级(1-4)。 */
    public static final StaticModifier<MerorResistanceModifier> merorResistance =
            MODIFIERS.register("meror_resistance", MerorResistanceModifier::new);

    /** 莫厄制式(精炼莫厄特性): 全套夜视/真视/力量/隐身/加速 + 减伤/免疫。 */
    public static final StaticModifier<MerorStandardModifier> merorStandard =
            MODIFIERS.register("meror_standard", MerorStandardModifier::new);

    /** 巨怪毛·护甲特性(御寒): 冰冻类伤害按件数减伤。 */
    public static final StaticModifier<GiantMonsterHairArmorModifier> giantMonsterHairArmor =
            MODIFIERS.register("giant_monster_hair_armor", GiantMonsterHairArmorModifier::new);

    /** 巨怪毛·近战特性(霜噬): 命中附加等量冰冻伤害。 */
    public static final StaticModifier<GiantMonsterHairMeleeModifier> giantMonsterHairMelee =
            MODIFIERS.register("giant_monster_hair_melee", GiantMonsterHairMeleeModifier::new);

    /** 贵族强化: 前置古二轮金属, 护甲阵营免疫, 近战命中召唤复魂尸。 */
    public static final StaticModifier<NobleUpgradeModifier> nobleUpgrade =
            MODIFIERS.register("noble_upgrade", NobleUpgradeModifier::new);

    /** 魔鳞升级: 前置鳞熊鳞片, 护甲元素/魔法减伤与全套效果。 */
    public static final StaticModifier<MagickaScaleUpgradeModifier> magickaScaleUpgrade =
            MODIFIERS.register("magicka_scale_upgrade", MagickaScaleUpgradeModifier::new);

    /** 鳞熊鳞片·特性: 对友好生物造成的伤害 +20%(每级)。 */
    public static final StaticModifier<ScalyBearScaleModifier> scalyBearScale =
            MODIFIERS.register("scaly_bear_scale", ScalyBearScaleModifier::new);
    /** 村民金属·特性(村民之力): 附近每只村民 +5% 攻击/防御。 */
    public static final StaticModifier<VillagerMetalModifier> villagerMetal =
            MODIFIERS.register("villager_metal", VillagerMetalModifier::new);

    /** 奴制监督者·捆缚: 近战命中几率捆住目标。 */
    public static final StaticModifier<ChainBindModifier> chainBind =
            MODIFIERS.register("chain_bind", ChainBindModifier::new);

    /** 不祥标枪·破盾: 弹射物命中停用目标盾牌。 */
    public static final StaticModifier<ShieldBreakModifier> shieldBreak =
            MODIFIERS.register("shield_break", ShieldBreakModifier::new);

    /** 火力倾泻者·链锯: 长按范围劈砍并造成流血。 */
    public static final StaticModifier<ChainsawModifier> chainsaw =
            MODIFIERS.register("chainsaw", ChainsawModifier::new);

    /** 火力倾泻者·弩: 5倍蓄力速度 + 弹射物无视受击冷却。 */
    public static final StaticModifier<FirepowerModifier> firepower =
            MODIFIERS.register("firepower", FirepowerModifier::new);

    /** 疯狂之斧: 长按旋风, 范围绞杀+吸血+拆方块, 释放后冷却。 */
    public static final StaticModifier<CrazyAxeModifier> crazyAxe =
            MODIFIERS.register("crazy_axe", CrazyAxeModifier::new);

    /** 苦寒之锤: 命中冰冻+破盾, 下落猛击追加伤害与范围击退。 */
    public static final StaticModifier<BitterColdHammerModifier> bitterColdHammer =
            MODIFIERS.register("bitter_cold_hammer", BitterColdHammerModifier::new);

    /** 大巫婆之帽(升级槽): 魔法减伤30% + 魔法吸收。 */
    public static final StaticModifier<BigWitchHatModifier> bigWitchHat =
            MODIFIERS.register("big_witchs_hat", BigWitchHatModifier::new);

    /** 鬼婆之眼(能力槽): 夜视 + 清失明/黑暗。 */
    public static final StaticModifier<HagEyeModifier> hagEye =
            MODIFIERS.register("hag_eye", HagEyeModifier::new);

    /** 紫沙鬼婆之眼(能力槽): 夜视 + 清失明/黑暗 + 看穿目标隐身。 */
    public static final StaticModifier<PurpleSandHagEyeModifier> purpleSandHagEye =
            MODIFIERS.register("purple_sand_hag_eye", PurpleSandHagEyeModifier::new);

    /** 鲙酷金属·特性: 佩戴/手持时水下呼吸。 */
    public static final StaticModifier<QoaikuMetalModifier> qoaikuMetal =
            MODIFIERS.register("qoaiku_metal", QoaikuMetalModifier::new);

    /** 骇浪三叉戟(能力槽): 蓄力施放宝石海浪法术。 */
    public static final StaticModifier<SeawaveTridentModifier> seawaveTrident =
            MODIFIERS.register("seawave_trident", SeawaveTridentModifier::new);

    /** 蛇龙合金·特性: 火系承伤-10%/输出+15%(每件)。 */
    public static final StaticModifier<SerponAlloyModifier> serponAlloy =
            MODIFIERS.register("serpon_alloy", SerponAlloyModifier::new);

    /** 蛇龙强者强化: 前置蛇龙合金, 火系承伤-15%/输出+20%(每件)。 */
    public static final StaticModifier<SerponPowerhouseModifier> serponPowerhouse =
            MODIFIERS.register("serpon_powerhouse", SerponPowerhouseModifier::new);

    /** 旱火野火盾牌(能力槽): 蓄力施放蛇龙火舌法术。 */
    public static final StaticModifier<DroughtWildfireShieldModifier> droughtWildfireShield =
            MODIFIERS.register("drought_wildfire_shield", DroughtWildfireShieldModifier::new);

    /** 明路巨兽角(无槽位): 近战伤害随目标体型增大。 */
    public static final StaticModifier<BrightLandBeastHornModifier> brightLandBeastHorn =
            MODIFIERS.register("bright_land_beast_horn", BrightLandBeastHornModifier::new);

    /** 明路之心(无槽位): 佩戴时提升最大生命值。 */
    public static final StaticModifier<BrightLandHeartModifier> brightLandHeart =
            MODIFIERS.register("bright_land_heart", BrightLandHeartModifier::new);


    /** 镇山·特性: 震慑/击飞/虚弱/清状态。 */
    public static final StaticModifier<MountainRealmModifier> mountainRealm =
            MODIFIERS.register("mountain_realm", MountainRealmModifier::new);

    /** 古二轮金属·特性: 复魂尸杀手(对沉降类生物追加伤害)。 */
    public static final StaticModifier<AncientSecondRounderModifier> ancientSecondRounder =
            MODIFIERS.register("ancient_second_rounder", AncientSecondRounderModifier::new);

    public static void register() {
    }
}
