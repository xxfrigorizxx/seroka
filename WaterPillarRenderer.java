package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.client.model.WaterPillarModel;
import com.seroka.entity.WaterPillarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/** Pilier d'eau translucide avec texture d'eau animée. */
public class WaterPillarRenderer extends EntityRenderer<WaterPillarEntity> {

  private static final ResourceLocation WATER_FLOW_SPRITE =
      ResourceLocation.withDefaultNamespace("block/water_flow");
  private static final int WATER_COLOR = 0xCC8CD2FF;

  private final WaterPillarModel model;

  public WaterPillarRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new WaterPillarModel(context.bakeLayer(WaterPillarModel.LAYER));
  }

  @Override
  public void render(
      WaterPillarEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(WATER_FLOW_SPRITE);
    float scroll = (entity.tickCount + partialTick) * 0.035F;
    float minY = (float) entity.getVisualBaseY(partialTick);
    float maxY = (float) entity.getVisualTopY(partialTick);

    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 8.0F));

    poseStack.translate(0.0D, entity.getRenderPivotYOffset(partialTick), 0.0D);

    poseStack.scale(
        WaterPillarEntity.VISUAL_SCALE,
        WaterPillarEntity.VISUAL_SCALE,
        WaterPillarEntity.VISUAL_SCALE
    );

    model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);

    VertexConsumer raw = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    VertexConsumer flowing = new FlowingSpriteVertexConsumer(raw, sprite, scroll, scroll * 0.5F);
    VertexConsumer vertices = new VerticalGradientVertexConsumer(flowing, minY, maxY, 0.5F);
    model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, WATER_COLOR);

    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(WaterPillarEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
