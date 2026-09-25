package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seroka.entity.EarthPillarEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
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

/** Rendu du pilier terrestre : colonne de vrais blocs Minecraft qui jaillit du sol. */
public class EarthPillarRenderer extends EntityRenderer<EarthPillarEntity> {

  /**
   * Un morceau de roche : position du centre de sa base, échelle uniforme et rotation.
   * L'échelle reste uniforme pour que la texture du bloc ne soit jamais étirée.
   */
  private record Chunk(float x, float y, float z, float scale, float yaw) {}

  /** Cinq blocs porteurs empilés jusqu'à 5.0, plus des éclats sur les flancs. */
  private static final Chunk[] CHUNKS = {
      new Chunk(0.00F, 0.00F, 0.00F, 1.00F, 0.0F),
      new Chunk(0.34F, 0.55F, 0.14F, 0.40F, 35.0F),
      new Chunk(0.05F, 1.00F, -0.04F, 1.00F, 13.0F),
      new Chunk(-0.32F, 1.60F, -0.16F, 0.36F, -25.0F),
      new Chunk(-0.04F, 2.00F, 0.05F, 1.00F, -9.0F),
      new Chunk(0.30F, 2.70F, -0.22F, 0.42F, 48.0F),
      new Chunk(0.04F, 3.00F, 0.03F, 1.00F, 16.0F),
      new Chunk(-0.28F, 3.55F, 0.20F, 0.34F, -40.0F),
      new Chunk(-0.05F, 4.00F, -0.03F, 1.00F, -12.0F),
      new Chunk(0.22F, 4.60F, 0.10F, 0.38F, 22.0F),
  };

  private final BlockRenderDispatcher blockRenderer;

  public EarthPillarRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.shadowRadius = 0.8F;
    this.blockRenderer = context.getBlockRenderDispatcher();
  }

  @Override
  public void render(
      EarthPillarEntity entity,
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

    float hauteur = entity.getHauteurRendu(partialTick);
    // Oriente chaque pilier différemment pour qu'ils ne soient pas tous identiques.
    float baseYaw = (entity.getId() * 47) % 360;

    poseStack.pushPose();
    // La colonne complète est décalée vers le bas : elle émerge progressivement du sol.
    poseStack.translate(0.0D, hauteur - EarthPillarEntity.HAUTEUR_TOTALE, 0.0D);
    poseStack.mulPose(Axis.YP.rotationDegrees(baseYaw));

    for (Chunk chunk : CHUNKS) {
      poseStack.pushPose();
      poseStack.translate(chunk.x(), chunk.y(), chunk.z());
      poseStack.mulPose(Axis.YP.rotationDegrees(chunk.yaw()));
      poseStack.scale(chunk.scale(), chunk.scale(), chunk.scale());
      poseStack.translate(-0.5D, 0.0D, -0.5D);

      // RenderType null : chaque couche du modèle du bloc choisit elle-même son type de rendu.
      this.blockRenderer.renderSingleBlock(
          state,
          poseStack,
          buffer,
          lumiereAuChunk(entity, chunk, hauteur, packedLight),
          OverlayTexture.NO_OVERLAY,
          ModelData.EMPTY,
          null
      );

      poseStack.popPose();
    }

    poseStack.popPose();

    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  /** Éclaire chaque morceau selon sa hauteur réelle : la base reste sombre, le sommet capte le ciel. */
  private int lumiereAuChunk(EarthPillarEntity entity, Chunk chunk, float hauteur, int packedLight) {
    double y = entity.getY() + hauteur - EarthPillarEntity.HAUTEUR_TOTALE + chunk.y();
    BlockPos pos = BlockPos.containing(entity.getX(), y + chunk.scale() * 0.5D, entity.getZ());
    int light = LevelRenderer.getLightColor(entity.level(), pos);
    // Un point de mesure enfoui dans un bloc opaque renverrait 0 et noircirait le morceau.
    return light == 0 ? packedLight : light;
  }

  @Override
  public ResourceLocation getTextureLocation(EarthPillarEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
