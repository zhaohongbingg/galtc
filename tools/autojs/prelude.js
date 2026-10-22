// galtc2 AutoJS 用例公共前置代码，由 run_all.py 拼到每个用例前面。
// 约定：用例文件定义 var CASE_DEF = { name, setup?, warmup?, measure }，
// 每一步都是一次独立的 eval（服务端在两次调用之间正常跑 tick），
// 现场由 park()/restore() 通过 AutoJS 的 JSON 文件在多次调用之间传递。
var GALT = (function () {
    var Collections = Java.loadClass('java.util.Collections');
    var UUID = Java.loadClass('java.util.UUID');
    var MaterialId = Java.loadClass('slimeknights.tconstruct.library.materials.definition.MaterialId');
    var MaterialRegistry = Java.loadClass('slimeknights.tconstruct.library.materials.MaterialRegistry');
    var MaterialVariant = Java.loadClass('slimeknights.tconstruct.library.materials.definition.MaterialVariant');
    var MaterialNBT = Java.loadClass('slimeknights.tconstruct.library.tools.nbt.MaterialNBT');
    var ToolBuildHandler = Java.loadClass('slimeknights.tconstruct.library.tools.helper.ToolBuildHandler');
    var TinkerTools = Java.loadClass('slimeknights.tconstruct.tools.TinkerTools');
    var ArmorItemType = Java.loadClass('net.minecraft.world.item.ArmorItem$Type');
    var ItemStack = Java.loadClass('net.minecraft.world.item.ItemStack');
    var CompoundTag = Java.loadClass('net.minecraft.nbt.CompoundTag');
    var EquipmentSlot = Java.loadClass('net.minecraft.world.entity.EquipmentSlot');
    var Attributes = Java.loadClass('net.minecraft.world.entity.ai.attributes.Attributes');
    var AttributeModifier = Java.loadClass('net.minecraft.world.entity.ai.attributes.AttributeModifier');
    var GameType = Java.loadClass('net.minecraft.world.level.GameType');
    var CombatRules = Java.loadClass('net.minecraft.world.damagesource.CombatRules');
    var Items = Java.loadClass('net.minecraft.world.item.Items');
    var BuiltInRegistries = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries');
    var ResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');

    var TEST_UUID = UUID.fromString('5b0c1f7e-2c4a-4d31-9e77-1a2b3c4d5e6f');
    var SITE_FILE = 'galtc2_autojs_site.json';
    var TAG = 'galtc2_autojs_test';
    var SLOTS = [EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET];

    function player() {
        return AutoJS.player();
    }

    /** 用单一材料造一件板甲（type 传 ArmorItem.Type 的名字：HELMET/CHESTPLATE/LEGGINGS/BOOTS） */
    function armorPiece(type, material) {
        var mid = new MaterialId(material);
        var material0 = MaterialRegistry.getInstance().getMaterial(mid);
        var nbt = new MaterialNBT(Collections.singletonList(MaterialVariant.of(material0)));
        var item = TinkerTools.plateArmor.get(ArmorItemType[type]);
        return ToolBuildHandler.buildItemFromMaterials(item, nbt);
    }

    /** 用单一材料造一件板甲（胸甲） */
    function chestplate(material) {
        return armorPiece('CHESTPLATE', material);
    }

    /** 四件套（同一材料），返回 [头, 胸, 腿, 靴] */
    function fullSet(material) {
        return [armorPiece('HELMET', material), armorPiece('CHESTPLATE', material),
            armorPiece('LEGGINGS', material), armorPiece('BOOTS', material)];
    }

    /** 穿满四件套 */
    function equipFullSet(material) {
        var set = fullSet(material);
        var p = player();
        p.setItemSlot(EquipmentSlot.HEAD, set[0]);
        p.setItemSlot(EquipmentSlot.CHEST, set[1]);
        p.setItemSlot(EquipmentSlot.LEGS, set[2]);
        p.setItemSlot(EquipmentSlot.FEET, set[3]);
        syncEquipment();
        return set;
    }

    /** 读工具栈 NBT 里某个键（调试 / 槽位校验用） */
    function stackTag(stack) {
        return String(stack.getTag());
    }

    function stackNbt(stack) {
        if (!stack || stack.isEmpty()) {
            return '';
        }
        return String(stack.save(new CompoundTag()));
    }

    function stackFromNbt(text) {
        if (!text) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(CompoundTagParser.parse(text));
    }

    // AutoJS 没有直接给 TagParser，这里用 Mojang 的 TagParser 读 SNBT
    var CompoundTagParser = (function () {
        var TagParser = Java.loadClass('net.minecraft.nbt.TagParser');
        return {
            parse: function (text) {
                return TagParser.parseTag(text);
            }
        };
    })();

    /** 玩家卡在死亡 / removed 状态时先把人救回来，返回当前可用的 ServerPlayer */
    function revive() {
        var p = player();
        if (p.isRemoved() || p.isDeadOrDying()) {
            var fresh = AutoJS.server().getPlayerList().respawn(p, false);
            if (fresh) {
                p = fresh;
            }
            p.setHealth(p.getMaxHealth());
            p.setGameMode(GameType.CREATIVE);
            return p;
        }
        return p;
    }

    /**
     * 存盘：游戏模式 / 血量 / 饥饿 / 四件护甲 / 主背包 + 副手，然后清场进入生存。
     * 玩家已经死了的话先救人再清场，而且不覆盖上一次的存档（死亡时背包会被清空，
     * 这时候写盘会把空背包存进去，之前的好存档就没了）。
     */
    function park() {
        var p = revive();
        var broken = p.isRemoved() || p.isDeadOrDying();
        var site = null;
        if (!broken) {
            site = {
                gameType: String(p.gameMode.getGameModeForPlayer().getName()),
                health: p.getHealth(),
                food: p.getFoodData().getFoodLevel(),
                armor: [],
                inventory: []
            };
            for (var i = 0; i < SLOTS.length; i++) {
                site.armor.push(stackNbt(p.getItemBySlot(SLOTS[i])));
                p.setItemSlot(SLOTS[i], ItemStack.EMPTY);
            }
            var items = p.getInventory().items;
            for (var s = 0; s < items.size(); s++) {
                site.inventory.push(stackNbt(items.get(s)));
            }
            var offhand = p.getInventory().offhand;
            for (var o = 0; o < offhand.size(); o++) {
                site.inventory.push(stackNbt(offhand.get(o)));
            }
            AutoJS.writeJson(SITE_FILE, site);
        } else {
            p.setItemSlot(SLOTS[0], ItemStack.EMPTY);
            p.setItemSlot(SLOTS[1], ItemStack.EMPTY);
            p.setItemSlot(SLOTS[2], ItemStack.EMPTY);
            p.setItemSlot(SLOTS[3], ItemStack.EMPTY);
        }

        syncEquipment();
        p.setGameMode(GameType.SURVIVAL);
        p.setHealth(p.getMaxHealth());
        p.setAbsorptionAmount(0);
        p.setRemainingFireTicks(0);
        p.invulnerableTime = 0;
        p.hurtTime = 0;
        clearBoost();
        clearLuck();
        return JSON.stringify({
            ok: true, parked: !broken, keptOldSite: broken,
            gameType: site ? site.gameType : '?', tick: serverTick()
        });
    }

    /** 服务端 tick 计数：调用方用它判断两次 eval 之间游戏到底有没有在跑（暂停时为 0 增长） */
    function serverTick() {
        try {
            return AutoJS.server().getTickCount();
        } catch (e) {
            return -1;
        }
    }

    /** 还原现场：护甲 / 背包 / 游戏模式 / 血量 / 临时属性 / 测试生物 */
    function restore() {
        var p = player();
        var site = AutoJS.readJson(SITE_FILE);
        var killed = sweepEntities();

        for (var i = 0; i < SLOTS.length; i++) {
            p.setItemSlot(SLOTS[i], stackFromNbt(site.armor[i]));
        }
        var items = p.getInventory().items;
        for (var s = 0; s < items.size(); s++) {
            items.set(s, stackFromNbt(site.inventory[s]));
        }
        var offhand = p.getInventory().offhand;
        for (var o = 0; o < offhand.size(); o++) {
            offhand.set(o, stackFromNbt(site.inventory[items.size() + o]));
        }
        syncEquipment();

        clearBoost();
        clearLuck();
        p.setGameMode(GameType.byName(site.gameType, GameType.CREATIVE));
        p.setHealth(site.health);
        p.getFoodData().setFoodLevel(site.food);
        p.setAbsorptionAmount(0);
        p.invulnerableTime = 0;
        p.hurtTime = 0;
        try {
            p.inventoryMenu.broadcastChanges();
        } catch (e) {
            // 客户端同步失败不影响结果
        }
        var armor = [];
        for (var a = 0; a < SLOTS.length; a++) {
            var stack = p.getItemBySlot(SLOTS[a]);
            armor.push(stack.isEmpty() ? '-' : String(stack.getHoverName().getString()));
        }
        return JSON.stringify({
            ok: true, restored: true, gameType: site.gameType, killed: killed, armor: armor, tick: serverTick()
        });
    }

    function sweepEntities() {
        var killed = 0;
        try {
            var server = AutoJS.server();
            var levels = server.getAllLevels().iterator();
            while (levels.hasNext()) {
                var level = levels.next();
                var entities = level.getAllEntities().iterator();
                while (entities.hasNext()) {
                    var entity = entities.next();
                    if (entity.getTags().contains(TAG)) {
                        entity.discard();
                        killed++;
                    }
                }
            }
        } catch (e) {
            return 'sweep failed: ' + String(e);
        }
        return killed;
    }

    /**
     * 手动触发一次装备刷新。
     * 正常情况下服务端每个 tick 会自己调用 LivingEntity#detectEquipmentUpdates，
     * 但玩家被 PlayerList.respawn 过之后实测不再自动跑，护甲属性会一直是 0
     * （这时测出来的伤害全是裸装数值）。这里显式补一次，结果与正常 tick 等价。
     */
    var DETECT_EQUIPMENT = (function () {
        var methods = Java.loadClass('java.lang.Class')
            .forName('net.minecraft.world.entity.LivingEntity').getDeclaredMethods();
        for (var i = 0; i < methods.length; i++) {
            if (String(methods[i].getName()) === 'detectEquipmentUpdates'
                && methods[i].getParameterTypes().length === 0) {
                methods[i].setAccessible(true);
                return methods[i];
            }
        }
        throw new Error('找不到 LivingEntity.detectEquipmentUpdates');
    })();

    function syncEquipment() {
        DETECT_EQUIPMENT.invoke(player(), []);
    }

    /**
     * 手动跑一遍护甲上的 INVENTORY_TICK 钩子。
     * 「按血量 / 幸运 / 背包钱数 / 面板护甲」实时算护甲的特性全靠这个钩子，
     * 正常由服务端 tick 驱动；但玩家被 respawn 过之后 tick 管线实测不再触发它们，
     * 所以读护甲相关数值前显式跑一次（调用的是同一份生产代码，不是测试专用逻辑）。
     */
    var ToolStackClass = Java.loadClass('slimeknights.tconstruct.library.tools.nbt.ToolStack');
    var ModifierManager = Java.loadClass('slimeknights.tconstruct.library.modifiers.ModifierManager');

    function armorInventoryTick() {
        var p = player();
        var slotDefs = [
            [EquipmentSlot.FEET, 36], [EquipmentSlot.LEGS, 37],
            [EquipmentSlot.CHEST, 38], [EquipmentSlot.HEAD, 39]
        ];
        for (var i = 0; i < slotDefs.length; i++) {
            var stack = p.getItemBySlot(slotDefs[i][0]);
            if (!stack || stack.isEmpty()) {
                continue;
            }
            try {
                var tool = ToolStackClass.from(stack);
                var entries = tool.getModifiers().getModifiers();
                for (var j = 0; j < entries.size(); j++) {
                    var entry = entries.get(j);
                    var modifier = ModifierManager.INSTANCE.get(entry.getId());
                    if (!modifier) {
                        continue;
                    }
                    // 直接调修饰器实例：走 tool.getHook(...) 拿到的合并钩子按 entry 身份分发，这里传进去会被跳过
                    modifier.onInventoryTick(tool, entry, p.level(), p, slotDefs[i][1],
                        false, true, stack);
                }
            } catch (e) {
                lastArmorTickError = String(e);
            }
        }
    }

    var lastArmorTickError = '';

    function clearArmor() {
        var p = player();
        for (var i = 0; i < SLOTS.length; i++) {
            p.setItemSlot(SLOTS[i], ItemStack.EMPTY);
        }
        syncEquipment();
    }

    function equipChest(stack) {
        player().setItemSlot(EquipmentSlot.CHEST, stack);
        syncEquipment();
    }

    /** 读护甲前先把「护甲 inventory tick」补跑一次，保证按血量/幸运/钱数算的特性已经结算 */
    function armorValue() {
        armorInventoryTick();
        return player().getAttributeValue(Attributes.ARMOR);
    }

    function toughness() {
        return player().getAttributeValue(Attributes.ARMOR_TOUGHNESS);
    }

    /** 护甲属性上 galtc2:* 修饰器的加成合计（特性实际给出的额外护甲） */
    function traitBonus() {
        armorInventoryTick();
        var arr = player().getAttribute(Attributes.ARMOR).getModifiers().toArray();
        var total = 0;
        var all = [];
        for (var i = 0; i < arr.length; i++) {
            var name = String(arr[i].getName());
            all.push(name + '=' + arr[i].getAmount());
            if (name.indexOf('galtc2:') === 0) {
                total += arr[i].getAmount();
            }
        }
        return { bonus: total, modifiers: all };
    }

    /** 特性以外的护甲来源（胸甲本身 + 测试用的临时加成） */
    function armorBase() {
        armorInventoryTick();
        var arr = player().getAttribute(Attributes.ARMOR).getModifiers().toArray();
        var total = 0;
        for (var i = 0; i < arr.length; i++) {
            if (String(arr[i].getName()).indexOf('galtc2:') !== 0) {
                total += arr[i].getAmount();
            }
        }
        return total;
    }

    /**
     * 与 ArmorAttributeHelper.baseArmor 同口径的「基础护甲」：四件护甲合起来的面板值，
     * 排除 galtc2:* 特性自己加的那部分。
     * 按修饰器逐项累加（不是 getArmorValue() 减自己），因为 getArmorValue() 已在属性上限处截断，
     * 顶过上限之后再减自己会得到负数，特性判定会每 tick 抖一次。
     */
    function baseArmor() {
        armorInventoryTick();
        var instance = player().getAttribute(Attributes.ARMOR);
        var addition = instance.getBaseValue();
        var multipliedBase = 0.0;
        var multipliedTotal = 1.0;
        var arr = instance.getModifiers().toArray();
        for (var i = 0; i < arr.length; i++) {
            if (String(arr[i].getName()).indexOf('galtc2:') === 0) {
                continue;
            }
            var op = String(arr[i].getOperation());
            if (op === 'ADDITION') {
                addition += arr[i].getAmount();
            } else if (op === 'MULTIPLY_BASE') {
                multipliedBase += arr[i].getAmount();
            } else if (op === 'MULTIPLY_TOTAL') {
                multipliedTotal *= 1.0 + arr[i].getAmount();
            }
        }
        return Math.max(0, Math.floor(addition * (1.0 + multipliedBase) * multipliedTotal));
    }

    /** ARMOR 属性的取值上限（原版 30，装了属性突破会被抬高）；读不到就当作无上限 */
    function armorCap() {
        try {
            return player().getAttribute(Attributes.ARMOR).getAttribute().getMaxValue();
        } catch (e) {
            return Infinity;
        }
    }

    function addArmorBoost(amount) {
        var instance = player().getAttribute(Attributes.ARMOR);
        instance.removeModifier(TEST_UUID);
        if (amount !== 0) {
            instance.addTransientModifier(new AttributeModifier(TEST_UUID, 'autojs:test_boost', amount,
                AttributeModifier.Operation.ADDITION));
        }
    }

    function clearBoost() {
        var instance = player().getAttribute(Attributes.ARMOR);
        if (instance.getModifier(TEST_UUID) != null) {
            instance.removeModifier(TEST_UUID);
        }
    }

    function addLuck(amount) {
        var instance = player().getAttribute(Attributes.LUCK);
        instance.removeModifier(TEST_UUID);
        if (amount !== 0) {
            instance.addTransientModifier(new AttributeModifier(TEST_UUID, 'autojs:test_luck', amount,
                AttributeModifier.Operation.ADDITION));
        }
    }

    function clearLuck() {
        var instance = player().getAttribute(Attributes.LUCK);
        if (instance.getModifier(TEST_UUID) != null) {
            instance.removeModifier(TEST_UUID);
        }
    }

    /**
     * actuallyHurt 是 protected，且必须走它而不是 hurt()：
     * 这个整合包里 LivingAttackEvent 会把「带实体的伤害」整条拦掉（player.hurt 直接返回 false），
     * 而改版护甲的减伤挂在 LivingHurtEvent 上，正好在 actuallyHurt 里面。
     */
    var ACTUALLY_HURT = (function () {
        var methods = Java.loadClass('java.lang.Class')
            .forName('net.minecraft.world.entity.LivingEntity').getDeclaredMethods();
        for (var i = 0; i < methods.length; i++) {
            if (String(methods[i].getName()) === 'actuallyHurt' && methods[i].getParameterTypes().length === 2) {
                methods[i].setAccessible(true);
                return methods[i];
            }
        }
        throw new Error('找不到 LivingEntity.actuallyHurt');
    })();

    var FloatBox = Java.loadClass('java.lang.Float');

    /**
     * 打一次固定伤害，返回实际掉血量。
     * 必须给 attacker（用 dummy() 就行）：minecraft:generic 属于 bypasses_armor，
     * 用它测护甲会得到「护甲完全没生效」的假结论。
     */
    function hurt(amount, attacker) {
        var p = player();
        if (amount >= p.getMaxHealth()) {
            throw new Error('测试伤害 ' + amount + ' >= 玩家最大生命 ' + p.getMaxHealth()
                + '，可能把玩家打死（死亡会清背包）。要测致死档位请用 hurtOther() 打测试生物。');
        }
        p.invulnerableTime = 0;
        p.hurtTime = 0;
        p.setAbsorptionAmount(0);
        p.setHealth(p.getMaxHealth());
        var before = p.getHealth();
        var source = attacker ? p.damageSources().mobAttack(attacker) : p.damageSources().cactus();
        var armorStage = CombatRules.getDamageAfterAbsorb(amount, p.getArmorValue(),
            p.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        ACTUALLY_HURT.invoke(p, source, FloatBox.valueOf(amount));
        var taken = before - p.getHealth();
        lastHurt = {
            amount: amount, taken: taken, armorStage: armorStage,
            armor: p.getArmorValue(), tough: p.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
        };
        return taken;
    }

    /**
     * 最近一次 hurt 的中间值，只在用例失败时由 report() 带出来。
     * 本整合包的伤害管线偶尔会在「护甲+附魔结算之后」再乘一个约 0.90 的系数
     * （与 galtc2 特性无关，见 run_all.py 的重试逻辑），有这组数就能一眼认出是不是它。
     */
    var lastHurt = null;

    /** 中立的攻击者（护甲 0、血量 20），只用它把伤害走「生物攻击」通道 */
    function dummy() {
        return summon('minecraft:armor_stand', 2, 0);
    }

    /** 给普通生物穿装备（用 EquipmentSlot 常量名） */
    function equipMob(entity, slotName, stack) {
        entity.setItemSlot(EquipmentSlot[slotName], stack);
    }

    /** 对非玩家生物打一次固定伤害，返回掉血量（判定一击必杀的分支用，避免玩家真死） */
    function hurtOther(entity, amount) {
        entity.invulnerableTime = 0;
        entity.hurtTime = 0;
        entity.setHealth(entity.getMaxHealth());
        var before = entity.getHealth();
        ACTUALLY_HURT.invoke(entity, entity.damageSources().cactus(), FloatBox.valueOf(amount));
        return before - entity.getHealth();
    }

    /** 统计背包里的某种物品数量（主背包 + 副手，与 ShochouArmor.countMoney 同口径） */
    function count(id) {
        var target = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
        var total = 0;
        var items = player().getInventory().items;
        for (var i = 0; i < items.size(); i++) {
            var stack = items.get(i);
            if (!stack.isEmpty() && stack.is(target)) {
                total += stack.getCount();
            }
        }
        var offhand = player().getInventory().offhand;
        for (var o = 0; o < offhand.size(); o++) {
            var off = offhand.get(o);
            if (!off.isEmpty() && off.is(target)) {
                total += off.getCount();
            }
        }
        return total;
    }

    /** 往空槽里塞 count 个物品，返回真正塞进去的数量（已经塞满就用返回的合计去算期望） */
    function fillSlots(id, total) {
        var items = player().getInventory().items;
        var left = total;
        for (var i = 0; i < items.size() && left > 0; i++) {
            if (!items.get(i).isEmpty()) {
                continue;
            }
            var batch = Math.min(64, left);
            items.set(i, item(id, batch));
            left -= batch;
        }
        return total - left;
    }

    function boostEntityArmor(entity, amount) {
        var instance = entity.getAttribute(Attributes.ARMOR);
        instance.addTransientModifier(new AttributeModifier(UUID.randomUUID(), 'autojs:test_mob_armor', amount,
            AttributeModifier.Operation.ADDITION));
    }

    function setHealthRatio(ratio) {
        var p = player();
        p.setHealth(Math.max(1.0, p.getMaxHealth() * ratio));
    }

    /**
     * 原版伤害减免算的是 floor(getArmorValue())，不是带小数的属性值；
     * 另外 Attributes.ARMOR 的取值上限就是 30，超过的部分属性面板直接截断。
     */
    function expectedAfterArmor(amount) {
        return CombatRules.getDamageAfterAbsorb(amount, Math.floor(armorValue()), toughness());
    }

    /** ModifyDamageModifierHook（jvt 那批特性）作用在护甲减免之前：先砍数值再走护甲公式 */
    function expectedPreReduction(amount, reduction) {
        return expectedAfterArmor(amount * (1.0 - reduction));
    }

    /** galtc2 的 onModifyTakeDamage 作用在护甲减免之后：先走护甲公式再乘 */
    function expectedPostReduction(amount, reduction) {
        return expectedAfterArmor(amount) * (1.0 - reduction);
    }

    /** 走原版 hurt()（会被 LivingAttackEvent 拦，但对仙人掌这类无实体来源是通的），用来测无敌帧 */
    function hurtVanilla(amount) {
        var p = player();
        if (amount >= p.getMaxHealth()) {
            throw new Error('测试伤害 ' + amount + ' >= 玩家最大生命 ' + p.getMaxHealth()
                + '，可能把玩家打死（死亡会清背包）。');
        }
        p.invulnerableTime = 0;
        p.hurtTime = 0;
        p.setAbsorptionAmount(0);
        p.setHealth(p.getMaxHealth());
        var before = p.getHealth();
        var applied = p.hurt(p.damageSources().cactus(), amount);
        return { taken: before - p.getHealth(), applied: applied, invulnerableTime: p.invulnerableTime };
    }

    function near(a, b, eps) {
        eps = eps === undefined ? 0.02 : eps;
        return Math.abs(a - b) <= eps;
    }

    function check(list, label, expected, actual, ok) {
        list.push({
            label: label,
            expected: expected,
            actual: actual,
            ok: ok === undefined ? near(Number(expected), Number(actual)) : ok
        });
    }

    function report(name, checks, notes) {
        var pass = true;
        for (var i = 0; i < checks.length; i++) {
            if (!checks[i].ok) {
                pass = false;
            }
        }
        notes = notes || [];
        if (!pass && lastHurt) {
            notes.push('伤害诊断: 面板 ' + lastHurt.armor + ' / 韧性 ' + lastHurt.tough
                + ' / 输入 ' + lastHurt.amount + ' → 护甲段应得 ' + Math.round(lastHurt.armorStage * 1000) / 1000
                + ' / 实际掉血 ' + Math.round(lastHurt.taken * 1000) / 1000);
        }
        return JSON.stringify({ case: name, pass: pass, checks: checks, notes: notes });
    }

    /** 调试用：只返回一个 ok 的中间步骤结果 */
    function staged(name, extra) {
        return JSON.stringify({ case: name, pass: true, staged: true, notes: [extra || ''] });
    }

    function summon(type, relX, relZ) {
        var p = player();
        var entity = AutoJS.summon(type, p.getX() + (relX || 3), p.getY(), p.getZ() + (relZ || 0));
        if (entity) {
            entity.addTag(TAG);
        }
        return entity;
    }

    function item(id, amount) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id)), amount);
    }

    return {
        player: player,
        chestplate: chestplate,
        armorPiece: armorPiece,
        fullSet: fullSet,
        equipFullSet: equipFullSet,
        stackTag: stackTag,
        park: park,
        restore: restore,
        sweepEntities: sweepEntities,
        clearArmor: clearArmor,
        equipChest: equipChest,
        armorValue: armorValue,
        toughness: toughness,
        traitBonus: traitBonus,
        armorBase: armorBase,
        baseArmor: baseArmor,
        armorCap: armorCap,
        addArmorBoost: addArmorBoost,
        clearBoost: clearBoost,
        addLuck: addLuck,
        clearLuck: clearLuck,
        hurt: hurt,
        hurtOther: hurtOther,
        equipMob: equipMob,
        dummy: dummy,
        count: count,
        fillSlots: fillSlots,
        boostEntityArmor: boostEntityArmor,
        setHealthRatio: setHealthRatio,
        expectedAfterArmor: expectedAfterArmor,
        expectedPreReduction: expectedPreReduction,
        expectedPostReduction: expectedPostReduction,
        hurtVanilla: hurtVanilla,
        near: near,
        check: check,
        report: report,
        staged: staged,
        summon: summon,
        item: item,
        Items: Items,
        ItemStack: ItemStack,
        Attributes: Attributes
    };
})();

/** run_all.py 通过它调度某一步 */
function __dispatch(def, step) {
    if (typeof def[step] !== 'function') {
        return JSON.stringify({ case: def.name, pass: true, skipped: step });
    }
    return def[step]();
}
