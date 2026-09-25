package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seroka.entity.IcePrisonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Bloc de glace translucide englobant la cible emprisonnée. */
public class IcePrisonRenderer extends EntityRenderer<IcePrisonEntity> {

  private static final float ECHELLE_X = IcePrisonEntity.VISUAL_ECHELLE_X;
  private static final float ECHELLE_Y = IcePrisonEntity.VISUAL_ECHELLE_Y;
  private static final float ECHELLE_Z = IcePrisonEntity.VISUAL_ECHELLE_Z;

  private final BlockRenderDispatcher blockRenderer;

  public IcePrisonRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.blockRenderer = context.getBlockRenderDispatcher();
  }

  @Override
  public void render(
      IcePrisonEntity entite,
      float yaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    BlockState etatGlace = Blocks.ICE.defaultBlockState();

    poseStack.pushPose();
    poseStack.translate(-0.5D, 0.0D, -0.5D);
    poseStack.scale(ECHELLE_X, ECHELLE_Y, ECHELLE_Z);

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
    super.render(entite, yaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(IcePrisonEntity entite) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
