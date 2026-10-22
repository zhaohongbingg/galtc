package com.galtc2.compat.bomd.client;

import com.cerbon.bosses_of_mass_destruction.util.VanillaCopies;
import com.cerbon.cerbons_api.api.static_utilities.Vec3Colors;
import com.galtc2.compat.bomd.BomdModifiers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 祐天寺美汐的实心光束（客户端）。
 * <p>
 * 完全照 BOMD 的下界铁掌激光来：贴图、RenderType、绘制方法都和
 * {@code GauntletLaserRenderer} 用的是同一套 ——
 * <pre>
 *   laserTexture = bosses_of_mass_destruction:textures/entity/gauntlet_beam.png
 *   type         = RenderType.entityCutoutNoCull(laserTexture)
 *   VanillaCopies.renderBeam(entity, start, end, partialTicks, Vec3Colors.LASER_RED, poseStack, buffer, type)
 * </pre>
 * 唯一不同的是"谁在放激光"：BOMD 那边读 {@code GauntletEntity.laserHandler}，
 * 这边直接看客户端手里的状态 —— 玩家 {@link Player#isUsingItem()} 且手上那把匠魂工具带
 * {@code blazing_eye}，就按他的视线画一条。**所以不需要任何自定义网络包**。
 * <p>
 * <b>资源说明</b>：贴图只是用 {@link ResourceLocation} 指向 BOMD jar 里的文件，
 * 运行时从它的 jar 读，没有复制到 galtc2，因此不触发 LGPL-3.0 的分发义务。
 * 代价是"必须装了 BOMD 才能画" —— 这正是我们要的：本类 import 了 BOMD / cerbons_api 的类，
 * 所以它只在 {@code BomdCompat.isLoaded()} 为真时才会被触碰（见 {@code Galtc2.ClientModEvents}），
 * 没装 BOMD 时它根本不会被加载。
 */
public final class BlazingEyeLaserRenderer {

    /** BOMD 的光束贴图（直接引用它 jar 里的文件） */
    private static final ResourceLocation LASER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("bosses_of_mass_destruction", "textures/entity/gauntlet_beam.png");
    /** 和 BOMD 的 GauntletLaserRenderer 用同一个 RenderType */
    private static final RenderType LASER_TYPE = RenderType.entityCutoutNoCull(LASER_TEXTURE);
    /** 光束长度：与 LaserAction.extendLaser 的 30 格对齐 */
    private static final double RANGE = 30.0;

    private BlazingEyeLaserRenderer() {
    }

    /**
     * 注册渲染监听。
     * <p>
     * <b>只能在确认装了 BOMD 之后调用</b>（本类引用了 BOMD / cerbons_api 的类型）。
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(BlazingEyeLaserRenderer::onRenderLevelStage);
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 与 BOMD 一样在实体阶段之后画：这样光束会正确地盖在实体上、被方块正常遮挡
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }

        float partialTick = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        boolean drew = false;

        Vec3 cameraPos = event.getCamera().getPosition();
        for (Player player : level.players()) {
            if (!isFiringLaser(player)) {
                continue;
            }
            Vec3 start = player.getEyePosition(partialTick);
            Vec3 end = start.add(player.getLookAngle().scale(RANGE));
            // 被方块挡住就截断（服务端那边也是这么算的，两边视觉一致）
            BlockHitResult hit = level.clip(new ClipContext(start, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                end = hit.getLocation();
            }

            // renderBeam 是从原版信标光束改的：内部只做 "poseStack.translate(0, entity.getBbHeight(), 0)"，
            // 而方向和长度是用**世界坐标**算的 —— 所以它要求 poseStack 的原点已经落在「实体所在位置」。
            // BOMD 是在实体渲染器（IRenderer<GauntletEntity>）里调的，那边 EntityRenderer 已经把
            // poseStack 挪到实体位置了；我们在 RenderLevelStageEvent 里原点还在相机上，
            // 不补这一步，整条光束就会浮在相机上方并且错位。
            double px = Mth.lerp(partialTick, player.xo, player.getX());
            double py = Mth.lerp(partialTick, player.yo, player.getY());
            double pz = Mth.lerp(partialTick, player.zo, player.getZ());
            poseStack.pushPose();
            poseStack.translate(px - cameraPos.x,
                    // 再补掉「身高 → 眼睛」那点差值，让光束从准星高度出来，和服务端的判定起点对齐
                    py - cameraPos.y + (player.getEyeHeight() - player.getBbHeight()),
                    pz - cameraPos.z);

            // 起终点传同一个瞄准点：renderBeam 会把这两个点做一次插值，传一样的就等于不额外插值，
            // 方向/长度完全由它内部的 delta 算，不会忽长忽短。
            VanillaCopies.renderBeam(player, end, end, partialTick, Vec3Colors.LASER_RED,
                    poseStack, buffer, LASER_TYPE);
            poseStack.popPose();
            drew = true;
        }

        if (drew) {
            buffer.endBatch(LASER_TYPE);
        }
    }

    /** 这个玩家是不是正在用「带祐天寺美汐的匠魂工具」—— 客户端本地状态就能判断，无需同步 */
    private static boolean isFiringLaser(Player player) {
        if (!player.isUsingItem() || player.getUseItem() != player.getMainHandItem()) {
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ModifiableItem)) {
            return false;
        }
        IToolStackView tool = ToolStack.from(stack);
        return tool.getModifierLevel(BomdModifiers.blazingEye.getId()) > 0;
    }
}
