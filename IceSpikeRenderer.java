package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.client.model.IceSpikeModel;
import com.seroka.entity.IceSpikeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class IceSpikeRenderer extends EntityRenderer<IceSpikeEntity> {

  private static final ResourceLocation TEXTURE =
      ResourceLocation.withDefaultNamespace("textures/block/packed_ice.png");

  private final IceSpikeModel model;

  public IceSpikeRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new IceSpikeModel(context.bakeLayer(IceSpikeModel.LAYER));
  }

  @Override
  public void render(
      IceSpikeEntity entity,
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

    VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
    this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
    this.model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, -1);
    poseStack.popPose();

    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(IceSpikeEntity entity) {
    return TEXTURE;
  }
}
