package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seroka.entity.LameSismiqueEntity;
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
import net.neoforged.neoforge.client.model.data.ModelData;

/** Lame tellurique haute (2,8) et très mince (0,3) qui glisse au sol. */
public class LameSismiqueRenderer extends EntityRenderer<LameSismiqueEntity> {

  private final BlockRenderDispatcher blockRenderer;

  public LameSismiqueRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.blockRenderer = context.getBlockRenderDispatcher();
    this.shadowRadius = 0.5F;
  }

  @Override
  public void render(
      LameSismiqueEntity entity,
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

    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getVisualYaw()));
    // X : 0,35 ≈ 0,3 bloc de large ; Y : 2,8 blocs de haut ; Z : profondeur effilée.
    poseStack.scale(0.35F, 2.8F, 1.8F);
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
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(LameSismiqueEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
