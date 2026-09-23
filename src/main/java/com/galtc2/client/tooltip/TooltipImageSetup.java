package com.galtc2.client.tooltip;

import com.galtc2.Galtc2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 把 {@link TooltipImageComponent} 登记给 Forge —— 少了这一步，提示框一画到它就崩。
 * <p>
 * Forge 47.4 起，{@code ClientTooltipComponent.create} 不再判断"组件有没有实现
 * {@code ClientTooltipComponent} 接口"，而是拿组件的<b>具体类</b>去
 * {@code net.minecraftforge.client.gui.ClientTooltipComponentManager} 里查工厂：
 * <pre>
 *   TooltipComponent
 *       → ClientTooltipComponent.create(component)
 *       → ClientTooltipComponentManager.createClientTooltipComponent(component)  // 按 component.getClass() 查表
 *       → 查不到就抛 IllegalArgumentException("Unknown TooltipComponent")
 * </pre>
 * 所以自定义组件必须在这里登记一次：工厂返回的实例就是要参与排版和绘制的客户端组件，
 * 我们的组件自己实现了 {@code ClientTooltipComponent}，直接返回原实例即可。
 * <p>
 * 事件走的是 <b>MOD 事件总线</b>（{@link Mod.EventBusSubscriber.Bus#MOD}），
 * 由 {@code ClientTooltipComponentManager.init()} 在客户端初始化时发出，
 * 早于任何提示框绘制。
 */
@Mod.EventBusSubscriber(modid = Galtc2.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TooltipImageSetup {

    private TooltipImageSetup() {
    }

    @SubscribeEvent
    public static void onRegisterClientTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        // 组件本身就是客户端组件，不需要包装
        event.register(TooltipImageComponent.class, component -> component);
    }
}
