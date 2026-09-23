package com.galtc2.client.tooltip;

import com.galtc2.Galtc2;
import com.mojang.datafixers.util.Either;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 带配置图片的物品：只往提示框的组件列表里<b>插一项</b> {@link TooltipImageComponent}，
 * 排版和绘制全部交给原版（或接管提示框渲染的那个模组）。
 * <p>
 * 为什么不用 {@code RenderTooltipEvent.Pre} + 自己重画：
 * 那条路要先 {@code setCanceled(true)} 取消原版绘制、再手工照抄一遍原版的排版，
 * 于是就和"自己接管提示框渲染"的模组（例如 ModernUI）各画一次，
 * 表现就是<b>两层提示框</b>。换成 GatherComponents 后不再取消任何东西，只加一个组件，
 * 谁负责画提示框就由谁画，永远只有一层；顺带也让其它改色 / 加行的提示框模组更兼容。
 */
@Mod.EventBusSubscriber(modid = Galtc2.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TooltipImageHandler {

    private TooltipImageHandler() {
    }

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        TooltipImageRegistry.Entry entry = TooltipImageRegistry.get(event.getItemStack());
        if (entry == null) {
            return;
        }
        // top = 插在最上面，否则插到最后（也就是文字下面）
        int index = entry.top() ? 0 : event.getTooltipElements().size();
        event.getTooltipElements().add(index, Either.right(entry.component()));
    }
}
