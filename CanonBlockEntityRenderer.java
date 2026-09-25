package com.seroka.client.renderer;

import com.seroka.block.CanonBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class CanonBlockEntityRenderer implements BlockEntityRenderer<CanonBlockEntity> {

  private final BlockEntityRendererProvider.Context contexte;

  public CanonBlockEntityRenderer(BlockEntityRendererProvider.Context contexte) {
    this.contexte = contexte;
  }

  @Override
  public void render(CanonBlockEntity entite, float partialTick, PoseStack poseStack,
      MultiBufferSource buffer, int packedLight, int packedOverlay) {
    CanonAssemblyRenderer.dessinerMonde(entite, poseStack, buffer,
        contexte.getBlockRenderDispatcher(), packedLight, partialTick);
  }
}
