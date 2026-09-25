package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.client.model.WaterCocoonModel;
import com.seroka.entity.WaterCocoonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Dôme d'eau translucide enveloppant le joueur. */
public class WaterCocoonRenderer extends EntityRenderer<WaterCocoonEntity> {

  private static final ResourceLocation WATER_FLOW_SPRITE =
      ResourceLocation.withDefaultNamespace("block/water_flow");
  private static final int WATER_COLOR = 0xAA8CD2FF;

  private final WaterCocoonModel model;

  public WaterCocoonRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new WaterCocoonModel(context.bakeLayer(WaterCocoonModel.LAYER));
  }

  @Override
  public void render(
      WaterCocoonEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    LivingEntity target = entity.getTarget();
    float width = target != null ? target.getBbWidth() : 0.6F;
    float height = target != null ? target.getBbHeight() : 1.8F;
    float envelope = Math.max(width, height) * 0.72F + 0.35F;

    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(WATER_FLOW_SPRITE);
    float scroll = (entity.tickCount + partialTick) * 0.02F;

    poseStack.pushPose();
    poseStack.scale(envelope, envelope, envelope);
    poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 1.5F));

    model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);

    VertexConsumer raw = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    VertexConsumer vertices = new FlowingSpriteVertexConsumer(raw, sprite, scroll, scroll * 0.5F);
    model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, WATER_COLOR);

    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(WaterCocoonEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
