package com.seroka.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.faction.ArmureSangCastHelper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Couche de rendu sang translucide pour entités vivantes portant l'Armure de Sang. */
@OnlyIn(Dist.CLIENT)
public class ArmureSangLivingLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

  private static final ResourceLocation TEXTURE_EAU =
      ResourceLocation.withDefaultNamespace("textures/block/water_still.png");
  private static final float DILATATION = 0.08F;
  private static final int COULEUR_SANG = 0xD0FF0015;

  public ArmureSangLivingLayer(RenderLayerParent<T, M> parent) {
    super(parent);
  }

  @Override
  public void render(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      T entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
  ) {
    if (!ArmureSangCastHelper.estActive(entity)) {
      return;
    }

    M model = this.getParentModel();
    VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE_EAU));

    poseStack.pushPose();
    poseStack.scale(1.0F + DILATATION, 1.0F + DILATATION, 1.0F + DILATATION);
    model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, COULEUR_SANG);
    poseStack.popPose();
  }
}
