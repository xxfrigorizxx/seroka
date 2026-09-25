package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seroka.entity.MeteoreTelluriqueEntity;
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

/** Rendu d'un rocher tellurique : amas 2x2x2 de blocs qui tourne sur lui-même. */
public class MeteoreTelluriqueRenderer extends EntityRenderer<MeteoreTelluriqueEntity> {

  private static final float SPIN_PER_TICK = 4.0F;

  private final BlockRenderDispatcher blockRenderer;

  public MeteoreTelluriqueRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.blockRenderer = context.getBlockRenderDispatcher();
    this.shadowRadius = 0.9F;
  }

  @Override
  public void render(
      MeteoreTelluriqueEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    BlockState state = entity.getBlockState();
    if (state.getRenderShape() == RenderShape.MODEL) {
      BlockPos pos = entity.blockPosition();
      int light = LevelRenderer.getLightColor(entity.level(), pos);
      if (light == 0) {
        light = packedLight;
      }

      float age = entity.tickCount + partialTick;
      float spin = (entity.getId() * 29 + age * SPIN_PER_TICK) % 360.0F;

      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(spin));
      poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.35F));
      // Amas 2x2x2 recentré sur l'entité.
      poseStack.translate(-1.0D, -1.0D, -1.0D);

      for (int dx = 0; dx < 2; dx++) {
        for (int dy = 0; dy < 2; dy++) {
          for (int dz = 0; dz < 2; dz++) {
            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
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
      }
      poseStack.popPose();
    }
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(MeteoreTelluriqueEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
