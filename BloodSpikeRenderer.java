package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.ModMain;
import com.seroka.client.model.IceSpikeModel;
import com.seroka.entity.BloodSpikeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/** Rendu du Pic de Sang — modèle 3D teinté rouge rubis vif. */
public class BloodSpikeRenderer extends EntityRenderer<BloodSpikeEntity> {

  private static final ResourceLocation TEXTURE =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "textures/entity/water_ball.png");
  private static final int BLOOD_COLOR = FastColor.ARGB32.color(0xFF, 0xFF, 0x10, 0x20);

  private final IceSpikeModel model;

  public BloodSpikeRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new IceSpikeModel(context.bakeLayer(IceSpikeModel.LAYER));
  }

  @Override
  public void render(
      BloodSpikeEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    poseStack.pushPose();
    poseStack.scale(0.30F, 0.30F, 0.30F);

    float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
    float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
    poseStack.mulPose(Axis.YP.rotationDegrees(yRot + 180.0F));
    poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
    poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));

    VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
    this.model.setupAnim(null, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
    this.model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, BLOOD_COLOR);
    poseStack.popPose();

    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(BloodSpikeEntity entity) {
    return TEXTURE;
  }
}
