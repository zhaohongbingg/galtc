package com.galtc2.modifiers;


import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;



public class Rude extends Modifier implements MeleeDamageModifierHook {
        public Rude(){}
        //注册Hook标识符
        //这步是必须,不然接口衍生的方法体无效
        @Override
        protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
            //注册implement后根接口对应的ModifierHook标识符;
            hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
        }
        //MeleeDamage方法修改工具伤害
        @Override
        public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
            //将实体形参context.getLivingTarget()定义为名为enemy的一个LivingEntity，此步骤可以省略但是如果省略后面enemy要换为context.getLivingTarget()，实际应用的时候也可以直接模式变量更省事一些
            LivingEntity enemy=context.getLivingTarget();
            //检测该实体最大生命值是否为20点
            if(enemy!=null&&enemy.getMaxHealth()==20){
                //是，则输出原伤害+996
                return damage+20;
            }
            //否则输出这个原伤害
            return damage;
        }
    }

