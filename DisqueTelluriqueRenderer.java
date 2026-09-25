package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seroka.entity.DisqueTelluriqueEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Disque tellurique : une seule plaque aplatie qui tourne vite et ondule sur ses axes. */
public class DisqueTelluriqueRenderer extends EntityRenderer<DisqueTelluriqueEntity> {

  private static final float SPIN_PER_TICK = 34.0F;
  /** Amplitude du gîte de la plaque, en degrés : assez pour onduler sans casser la rotation. */
  private static final float WOBBLE_DEGREES = 7.0F;

  private final BlockRenderDispatcher blockRenderer;

  public DisqueTelluriqueRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.blockRenderer = context.getBlockRenderDispatcher();
    this.shadowRadius = 0.45F;
  }

  @Override
  public void render(
      DisqueTelluriqueEntity entity,
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

    float age = entity.tickCount + partialTick;
    float compression = entity.getCompression();

    poseStack.pushPose();

    // Rotation puis mise à l'échelle : l'axe fin suit le gîte, la plaque reste rigide.
    poseStack.mulPose(Axis.YP.rotationDegrees((age * SPIN_PER_TICK) % 360.0F));
    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(age * 0.35F) * WOBBLE_DEGREES * compression));
    poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.cos(age * 0.27F) * WOBBLE_DEGREES * compression));

    float breathe = Mth.sin(age * 0.5F) * 0.03F * compression;
    float scaleY = 1.0F - compression * 0.85F + breathe;
    float scaleXZ = 1.0F + compression * 0.35F - breathe;
    poseStack.scale(scaleXZ, scaleY, scaleXZ);

    poseStack.translate(-0.5D, -0.5D, -0.5D);
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
  public ResourceLocation getTextureLocation(DisqueTelluriqueEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
