package com.seroka.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.faction.ArmureEauCastHelper;
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

/** Couche de rendu d'eau translucide pour toutes les entités vivantes (villageois, animaux, mobs). */
@OnlyIn(Dist.CLIENT)
public class ArmureEauLivingLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

  private static final ResourceLocation TEXTURE_EAU =
      ResourceLocation.withDefaultNamespace("textures/block/water_still.png");
  private static final float DILATATION = 0.08F;
  private static final int COULEUR_EAU = 0xB080B8FF;

  public ArmureEauLivingLayer(RenderLayerParent<T, M> parent) {
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
    if (!ArmureEauCastHelper.estActive(entity)) {
      return;
    }

    M model = this.getParentModel();
    VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE_EAU));

    poseStack.pushPose();
    poseStack.scale(1.0F + DILATATION, 1.0F + DILATATION, 1.0F + DILATATION);
    model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, COULEUR_EAU);
    poseStack.popPose();
  }
}
