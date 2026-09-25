package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.ModMain;
import com.seroka.entity.WaterBallEntity;
import net.minecraft.client.model.WindChargeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Boule d'eau — échelle synchronisée via DATA_CHARGE (sphère dilatable). */
public class WaterBallRenderer extends EntityRenderer<WaterBallEntity> {

  private static final ResourceLocation TEXTURE =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "textures/entity/water_ball.png");

  private final WindChargeModel model;

  public WaterBallRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new WindChargeModel(context.bakeLayer(ModelLayers.WIND_CHARGE));
  }

  @Override
  public void render(
      WaterBallEntity entite,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    int charge = entite.getEntityData().get(WaterBallEntity.DATA_CHARGE);
    boolean enCharge = entite.getEntityData().get(WaterBallEntity.DATA_CHARGING);
    float scale;
    if (charge <= 0 && !enCharge) {
      scale = 1.0F;
    } else {
      scale = 0.5F + (Math.min(charge, 60.0F) / 60.0F) * 2.5F;
    }

    poseStack.pushPose();
    poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    poseStack.scale(scale, scale, scale);

    VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
    this.model.setupAnim(entite, partialTick, 0.0F, 0.0F, entite.getYRot(), entite.getXRot());
    this.model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY);
    poseStack.popPose();
  }

  @Override
  public ResourceLocation getTextureLocation(WaterBallEntity entite) {
    return TEXTURE;
  }
}
