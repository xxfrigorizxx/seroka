package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seroka.entity.EarthCrushEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Rendu 3D Écrasement Terrestre — Cubes pleins qui se soulèvent et écrasent la cible. */
public class EarthCrushRenderer extends EntityRenderer<EarthCrushEntity> {
  private final BlockRenderDispatcher blockRenderer;

  public EarthCrushRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.shadowRadius = 0.5F;
    this.blockRenderer = context.getBlockRenderDispatcher();
  }

  @Override
  public void render(
      EarthCrushEntity entity,
      float yaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    Level level = entity.level();
    BlockPos lightPos = BlockPos.containing(entity.getX(), entity.getY() + entity.getCibleHauteur() + 0.5D, entity.getZ());
    int tier = entity.getSizeTier();
    float width = Math.max(entity.getCibleLargeur(), 0.6F);
    float height = Math.max(entity.getCibleHauteur(), 1.0F);
    float age = entity.tickCount + partialTick;

    float currentYOffset;
    if (age < EarthCrushEntity.PHASE1_RISE_TICKS) {
      float riseProgress = Mth.clamp(age / (float) EarthCrushEntity.PHASE1_RISE_TICKS, 0.0F, 1.0F);
      float easedRise = Mth.sin(riseProgress * (float) Math.PI * 0.5F);
      currentYOffset = Mth.lerp(easedRise, -height * 0.8F, 0.0F);
    } else {
      currentYOffset = 0.0F;
    }

    float openOffset = width * 0.5F + tier * 0.8F + 0.5F;
    float closedOffset = 0.0F;
    float currentXOffset;
    if (age < EarthCrushEntity.PHASE1_RISE_TICKS) {
      currentXOffset = openOffset;
    } else {
      float slamProgress = Mth.clamp((age - EarthCrushEntity.PHASE1_RISE_TICKS) / (float) EarthCrushEntity.PHASE2_SLAM_TICKS, 0.0F, 1.0F);
      float easedSlam = slamProgress * slamProgress;
      currentXOffset = Mth.lerp(easedSlam, openOffset, closedOffset);
    }

    poseStack.pushPose();
    poseStack.translate(0.0D, -height * 0.5D, 0.0D);
    poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getCibleYRot()));

    int light = LevelRenderer.getLightColor(level, lightPos);
    if (light == 0) {
      light = packedLight;
    }

    renderGridVolume(level, blockRenderer, entity.getLeftGrid(), lightPos, poseStack, buffer, -currentXOffset, currentYOffset, tier, true, light);
    renderGridVolume(level, blockRenderer, entity.getRightGrid(), lightPos, poseStack, buffer, currentXOffset, currentYOffset, tier, false, light);

    poseStack.popPose();
    super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
  }

  private void renderGridVolume(
      Level level,
      BlockRenderDispatcher blockRenderer,
      BlockState[] grid,
      BlockPos lightPos,
      PoseStack poseStack,
      MultiBufferSource buffer,
      float offsetX,
      float offsetY,
      int tier,
      boolean leftSide,
      int light
  ) {
    if (grid == null || grid.length == 0) {
      return;
    }
    poseStack.pushPose();
    poseStack.translate(offsetX, offsetY, 0.0F);
    float halfTier = (tier - 1) / 2.0F;
    int idx = 0;
    for (int depth = 0; depth < tier; depth++) {
      float xShift = leftSide ? -depth : depth;
      for (int y = 0; y < tier; y++) {
        float yPos = y - halfTier;
        for (int z = 0; z < tier; z++) {
          float zPos = z - halfTier;
          BlockState state = (idx < grid.length) ? grid[idx++] : null;
          if (state != null && !state.isAir()) {
            poseStack.pushPose();
            poseStack.translate(-0.5D + xShift, yPos, zPos - 0.5D);
            BakedModel bakedModel = blockRenderer.getBlockModel(state);
            RandomSource random = RandomSource.create(state.getSeed(lightPos));
            for (RenderType renderType : bakedModel.getRenderTypes(state, random, ModelData.EMPTY)) {
              blockRenderer.renderSingleBlock(
                  state,
                  poseStack,
                  buffer,
                  light,
                  OverlayTexture.NO_OVERLAY,
                  ModelData.EMPTY,
                  renderType
              );
            }
            poseStack.popPose();
          }
        }
      }
    }
    poseStack.popPose();
  }

  @Override
  public ResourceLocation getTextureLocation(EarthCrushEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
