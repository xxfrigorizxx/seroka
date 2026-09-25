package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seroka.entity.EarthBlockEntity;
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

/** Rendu du bloc de terre lévité en vrai bloc Minecraft 3D. */
public class EarthBlockRenderer extends EntityRenderer<EarthBlockEntity> {
  private final BlockRenderDispatcher blockRenderer;

  public EarthBlockRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.shadowRadius = 0.5F;
    this.blockRenderer = context.getBlockRenderDispatcher();
  }

  @Override
  public void render(
      EarthBlockEntity entity,
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

      poseStack.pushPose();
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
  public ResourceLocation getTextureLocation(EarthBlockEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
