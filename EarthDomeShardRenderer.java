package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seroka.entity.EarthDomeShardEntity;
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

/** Rendu d'un éclat de terre en vol vers le Dôme de Terre. */
public class EarthDomeShardRenderer extends EntityRenderer<EarthDomeShardEntity> {

  private static final float SCALE = 0.8F;
  private static final float SPIN_PER_TICK = 14.0F;

  private final BlockRenderDispatcher blockRenderer;

  public EarthDomeShardRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.shadowRadius = 0.3F;
    this.blockRenderer = context.getBlockRenderDispatcher();
  }

  @Override
  public void render(
      EarthDomeShardEntity entity,
      float yaw,
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

      // Décalage par identifiant : les éclats ne tournent pas tous en phase.
      float spin = (entity.getId() * 37 + (entity.tickCount + partialTick) * SPIN_PER_TICK) % 360.0F;

      poseStack.pushPose();
      poseStack.scale(SCALE, SCALE, SCALE);
      poseStack.mulPose(Axis.YP.rotationDegrees(spin));
      poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.5F));
      poseStack.translate(-0.5D, -0.5D, -0.5D);
      this.blockRenderer.renderSingleBlock(
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
    super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(EarthDomeShardEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
