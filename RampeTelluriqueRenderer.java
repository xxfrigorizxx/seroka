package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seroka.entity.RampeTelluriqueEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Rampe tellurique 2×1×3 ancrée au sol, inclinée à 45°. */
public class RampeTelluriqueRenderer extends EntityRenderer<RampeTelluriqueEntity> {

  private final BlockRenderDispatcher blockRenderer;

  public RampeTelluriqueRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.blockRenderer = context.getBlockRenderDispatcher();
    this.shadowRadius = 1.2F;
  }

  @Override
  public void render(
      RampeTelluriqueEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    BlockState state = entity.getBlockState();
    if (state.getRenderShape() != RenderShape.MODEL) {
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      return;
    }

    BlockPos pos = entity.blockPosition();
    int light = LevelRenderer.getLightColor(entity.level(), pos);
    if (light == 0) {
      light = packedLight;
    }

    float progress = entity.getEmergeProgress(partialTick);

    poseStack.pushPose();

    // Recul dans l'axe de la pente : la rampe est éjectée du sol à 45°, pas verticalement.
    double slide = entity.getSlideOffset(progress);
    Vec3 dir = entity.getEmergeDirection();
    poseStack.translate(-dir.x * slide, -dir.y * slide, -dir.z * slide);
    poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getVisualYaw() + 180.0F));
    poseStack.mulPose(Axis.XP.rotationDegrees(entity.getVisualPitch()));

    for (int lengthIndex = 0; lengthIndex < RampeTelluriqueEntity.RAMP_LENGTH; lengthIndex++) {
      for (int widthIndex = 0; widthIndex < RampeTelluriqueEntity.RAMP_WIDTH; widthIndex++) {
        poseStack.pushPose();
        poseStack.translate(widthIndex - 0.5D, 0.0D, lengthIndex);
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        blockRenderer.renderSingleBlock(
            state,
            poseStack,
            buffer,
            light,
            OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY,
            RenderType.cutout()
        );
        poseStack.popPose();
      }
    }

    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(RampeTelluriqueEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
