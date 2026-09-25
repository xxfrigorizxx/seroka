package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.ModMain;
import com.seroka.entity.BloodBallEntity;
import net.minecraft.client.model.WindChargeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/** Rendu 3D de la Boule de Sang — sphère lisse rouge rubis translucide. */
public class BloodBallRenderer extends EntityRenderer<BloodBallEntity> {

  private static final ResourceLocation SPHERE_TEXTURE =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "textures/entity/water_ball.png");

  /** Teinte Rouge Rubis Vif (#FF1020) */
  private static final int BLOOD_COLOR = FastColor.ARGB32.color(0xFF, 0xFF, 0x10, 0x20);

  private final WindChargeModel model;

  public BloodBallRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new WindChargeModel(context.bakeLayer(ModelLayers.WIND_CHARGE));
  }

  @Override
  public void render(
      BloodBallEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    poseStack.pushPose();
    poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    poseStack.scale(1.2F, 1.2F, 1.2F);

    VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(SPHERE_TEXTURE));
    this.model.setupAnim(entity, partialTick, 0.0F, 0.0F, entity.getYRot(), entity.getXRot());
    this.model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, BLOOD_COLOR);

    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(BloodBallEntity entity) {
    return SPHERE_TEXTURE;
  }
}
