package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.entity.LavaBallEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.WindChargeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/** Boule de lave — textures vanilla lava_still / lava_flow animées. */
public class LavaBallRenderer extends EntityRenderer<LavaBallEntity> {

  private static final ResourceLocation SPRITE_LAVE_CALME =
      ResourceLocation.withDefaultNamespace("block/lava_still");
  private static final ResourceLocation SPRITE_LAVE_COURANTE =
      ResourceLocation.withDefaultNamespace("block/lava_flow");

  private static final int COULEUR_LAVE = 0xCCFF6A00;

  private final WindChargeModel model;

  public LavaBallRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new WindChargeModel(context.bakeLayer(ModelLayers.WIND_CHARGE));
  }

  @Override
  public void render(
      LavaBallEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    Minecraft client = Minecraft.getInstance();
    TextureAtlasSprite spriteCalme = client
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(SPRITE_LAVE_CALME);
    TextureAtlasSprite spriteCourante = client
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(SPRITE_LAVE_COURANTE);

    float age = entity.tickCount + partialTick;
    float scroll = age * 0.035F;

    poseStack.pushPose();
    poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    poseStack.scale(1.1F, 1.1F, 1.1F);

    int light = LightTexture.FULL_BRIGHT;
    RenderType renderType = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    VertexConsumer raw = buffer.getBuffer(renderType);

    VertexConsumer vertices = new FlowingSpriteVertexConsumer(
        raw,
        spriteCourante,
        scroll * 0.4F,
        scroll
    );
    this.model.setupAnim(entity, partialTick, 0.0F, 0.0F, entity.getYRot(), entity.getXRot());
    this.model.renderToBuffer(poseStack, vertices, light, OverlayTexture.NO_OVERLAY, COULEUR_LAVE);

    VertexConsumer centreVertices = new FlowingSpriteVertexConsumer(
        raw,
        spriteCalme,
        -scroll * 0.25F,
        scroll * 0.25F
    );
    this.model.renderToBuffer(poseStack, centreVertices, light, OverlayTexture.NO_OVERLAY, 0xCCCF2F0A);

    if (buffer instanceof MultiBufferSource.BufferSource immediate) {
      immediate.endBatch(renderType);
    }

    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(LavaBallEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
