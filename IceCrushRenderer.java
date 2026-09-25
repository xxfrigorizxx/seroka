package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seroka.entity.IceCrushEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Deux blocs de glace latéraux qui referment sur la cible. */
public class IceCrushRenderer extends EntityRenderer<IceCrushEntity> {

  /** Épaisseur minimale des pans de glace (mobs fins type squelette). */
  private static final float EPAisseur_MIN = 0.62F;
  private static final float FACTEUR_EPAisseur = 0.78F;

  private final BlockRenderDispatcher blockRenderer;

  public IceCrushRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.blockRenderer = context.getBlockRenderDispatcher();
  }

  @Override
  public void render(
      IceCrushEntity entite,
      float yaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    BlockState etatGlace = Blocks.ICE.defaultBlockState();
    float largeur = Math.max(entite.getCibleLargeur(), 0.6F);
    float hauteur = Math.max(entite.getCibleHauteur(), 1.0F);

    float echelleX = Math.max(largeur * FACTEUR_EPAisseur, EPAisseur_MIN);
    float echelleY = hauteur * 1.05F;
    float echelleZ = Math.max(largeur * FACTEUR_EPAisseur, EPAisseur_MIN);

    float progression = Mth.clamp(
        (entite.tickCount + partialTick) / IceCrushEntity.DUREE_ANIMATION_TICKS,
        0.0F,
        1.0F
    );
    float eased = 1.0F - (1.0F - progression) * (1.0F - progression);
    float ecartOuvert = largeur * 0.5F + echelleX + 0.35F;
    float ecartFerme = -echelleX * 0.18F;
    float ecart = Mth.lerp(eased, ecartOuvert, ecartFerme);

    poseStack.pushPose();
    poseStack.translate(0.0D, -hauteur * 0.5D, 0.0D);
    poseStack.mulPose(Axis.YP.rotationDegrees(-entite.getCibleYRot()));

    dessinerBlocGlace(
        poseStack,
        buffer,
        packedLight,
        blockRenderer,
        etatGlace,
        -ecart,
        echelleX,
        echelleY,
        echelleZ
    );
    dessinerBlocGlace(
        poseStack,
        buffer,
        packedLight,
        blockRenderer,
        etatGlace,
        ecart,
        echelleX,
        echelleY,
        echelleZ
    );

    poseStack.popPose();
    super.render(entite, yaw, partialTick, poseStack, buffer, packedLight);
  }

  private static void dessinerBlocGlace(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      BlockRenderDispatcher blockRenderer,
      BlockState etatGlace,
      double decalageHorizontal,
      float echelleX,
      float echelleY,
      float echelleZ
  ) {
    poseStack.pushPose();
    poseStack.translate(decalageHorizontal, 0.0D, 0.0D);
    poseStack.translate(-0.5D, 0.0D, -0.5D);
    poseStack.scale(echelleX, echelleY, echelleZ);

    blockRenderer.renderSingleBlock(
        etatGlace,
        poseStack,
        buffer,
        packedLight,
        OverlayTexture.NO_OVERLAY,
        ModelData.EMPTY,
        RenderType.translucent()
    );

    poseStack.popPose();
  }

  @Override
  public ResourceLocation getTextureLocation(IceCrushEntity entite) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
