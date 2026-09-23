package com.galtc2.client.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.HashSet;
import java.util.Set;

/**
 * 提示框里的一行「自定义图片」。
 * <p>
 * <b>必须先在 {@link TooltipImageSetup} 里注册</b>：Forge 47.4 之后
 * {@code ClientTooltipComponent.create} 是按组件的具体类查工厂的，
 * 光实现接口不注册会直接抛 {@code IllegalArgumentException: Unknown TooltipComponent}。
 * <p>
 * 它就是原版的 {@link ClientTooltipComponent}：把它塞进提示框的组件列表后，
 * <b>原版自己</b>会按 {@link #getWidth(Font)} / {@link #getHeight()} 排版（宽度也参与整框宽度计算），
 * 再在算好的位置上调用 {@link #renderImage(Font, int, int, GuiGraphics)} 把图贴出来。
 * <p>
 * 排版按 {@code width}×{@code height}（画布）算，所以<b>同一类（横屏/竖屏）的图片占的位置完全一致</b>；
 * 原图按比例缩成 {@code imageWidth}×{@code imageHeight} 在画布里居中，两边/上下的差是透明的，不会把图拉变形。
 * <p>
 * 图片资源用完整路径（含 {@code textures/} 与 {@code .png}），例如
 * {@code galtc2:textures/gui/tooltip/ciallo.png}。
 *
 * @param width       画布宽（参与提示框排版）
 * @param height      画布高
 * @param imageWidth  图片实际绘制的宽（已按原图比例缩好）
 * @param imageHeight 图片实际绘制的高
 * @param smooth      原图比绘制尺寸大 2 倍以上时用线性过滤（降采样更平滑），接近 1:1 时用 NEAREST（更锐利）
 */
public record TooltipImageComponent(ResourceLocation texture, int width, int height,
                                    int imageWidth, int imageHeight,
                                    boolean smooth) implements ClientTooltipComponent, TooltipComponent {

    /** 图片四周的留白，避免贴着提示框边框 */
    public static final int PADDING = 4;

    /** 已经设置过采样方式的纹理，避免每帧重复设置（会触发纹理重新上传） */
    private static final Set<ResourceLocation> FILTERED = new HashSet<>();

    @Override
    public int getHeight() {
        return height + PADDING * 2;
    }

    @Override
    public int getWidth(Font font) {
        return width + PADDING * 2;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        if (FILTERED.add(texture)) {
            // 1:1 附近用 NEAREST 保持锐利；大幅缩小用线性过滤，否则降采样会出斑点。
            // 只设一次，并且永远不打开 mipmap —— 纹理加载时没为它生成 mipmap 数据。
            try {
                Minecraft.getInstance().getTextureManager().getTexture(texture).setFilter(smooth, false);
            } catch (Exception ignored) {
                // 纹理缺失之类交给下面的 blit 处理，不要因此把客户端搞崩
            }
        }
        // 在画布里居中：整张贴图 1:1 画上去，UV 取满（0..1）
        int left = x + PADDING + (width - imageWidth) / 2;
        int top = y + PADDING + (height - imageHeight) / 2;
        graphics.blit(texture, left, top, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }
}
