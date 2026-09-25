package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.entity.WaterSiphonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/** Entonnoir d'eau 3D massif — volume fermé par segment (extérieur, intérieur, haut, bas). */
public class WaterSiphonRenderer extends EntityRenderer<WaterSiphonEntity> {

  private static final ResourceLocation WATER_FLOW_SPRITE =
      ResourceLocation.withDefaultNamespace("block/water_flow");
  /** Bleu eau translucide massif (#33A0FF avec alpha DD). */
  private static final int WATER_COLOR = FastColor.ARGB32.color(0xDD, 0x33, 0xA0, 0xFF);

  /** Épaisseur de la paroi d'eau (~3-4 pixels de volume massif). */
  private static final float WALL_THICKNESS = 0.20F;

  public WaterSiphonRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public void render(
      WaterSiphonEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(WATER_FLOW_SPRITE);
    float age = entity.tickCount + partialTick;
    float uScroll = age * 0.12F;
    float vScroll = age * 0.08F;

    float currentRadius = Math.max(0.1F, entity.getRadiusRatio(partialTick));
    float currentHeight = currentRadius * 0.8F;

    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(age * 18.0F));

    VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    renderSolidFunnelMesh(poseStack, vertices, sprite, uScroll, vScroll, currentRadius, currentHeight, packedLight);

    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  private void renderSolidFunnelMesh(
      PoseStack poseStack,
      VertexConsumer builder,
      TextureAtlasSprite sprite,
      float uScroll,
      float vScroll,
      float maxRadius,
      float maxHeight,
      int light
  ) {
    PoseStack.Pose entry = poseStack.last();
    int heightRings = 8;
    int segments = 24;
    float halfThick = WALL_THICKNESS / 2.0F;

    for (int r = 0; r < heightRings; r++) {
      float frac1 = (float) r / heightRings;
      float frac2 = (float) (r + 1) / heightRings;

      float baseR1 = 0.08F + (float) Math.pow(frac1, 1.3D) * maxRadius;
      float baseR2 = 0.08F + (float) Math.pow(frac2, 1.3D) * maxRadius;

      float r1Out = baseR1 + halfThick;
      float r1In = Math.max(0.01F, baseR1 - halfThick);
      float r2Out = baseR2 + halfThick;
      float r2In = Math.max(0.01F, baseR2 - halfThick);

      float y1 = frac1 * maxHeight;
      float y2 = frac2 * maxHeight;

      for (int i = 0; i < segments; i++) {
        float segFrac1 = (float) i / segments;
        float segFrac2 = (float) (i + 1) / segments;

        float angle1 = (float) (segFrac1 * Math.PI * 2.0D);
        float angle2 = (float) (segFrac2 * Math.PI * 2.0D);

        float minU1 = sprite.getU(Mth.frac(segFrac1 + uScroll));
        float maxU2 = sprite.getU(Mth.frac(segFrac2 + uScroll));
        float minV1 = sprite.getV(Mth.frac(frac1 + vScroll));
        float maxV2 = sprite.getV(Mth.frac(frac2 + vScroll));

        float cos1 = (float) Math.cos(angle1);
        float sin1 = (float) Math.sin(angle1);
        float cos2 = (float) Math.cos(angle2);
        float sin2 = (float) Math.sin(angle2);

        addQuad(
            builder, entry,
            cos1 * r1Out, y1, sin1 * r1Out,
            cos2 * r1Out, y1, sin2 * r1Out,
            cos2 * r2Out, y2, sin2 * r2Out,
            cos1 * r2Out, y2, sin1 * r2Out,
            minU1, maxU2, minV1, maxV2, light
        );

        addQuad(
            builder, entry,
            cos1 * r2In, y2, sin1 * r2In,
            cos2 * r2In, y2, sin2 * r2In,
            cos2 * r1In, y1, sin2 * r1In,
            cos1 * r1In, y1, sin1 * r1In,
            minU1, maxU2, minV1, maxV2, light
        );

        addQuad(
            builder, entry,
            cos1 * r2In, y2, sin1 * r2In,
            cos1 * r2Out, y2, sin1 * r2Out,
            cos2 * r2Out, y2, sin2 * r2Out,
            cos2 * r2In, y2, sin2 * r2In,
            minU1, maxU2, minV1, maxV2, light
        );

        addQuad(
            builder, entry,
            cos1 * r1Out, y1, sin1 * r1Out,
            cos1 * r1In, y1, sin1 * r1In,
            cos2 * r1In, y1, sin2 * r1In,
            cos2 * r1Out, y1, sin2 * r1Out,
            minU1, maxU2, minV1, maxV2, light
        );
      }
    }
  }

  private static void addQuad(
      VertexConsumer builder,
      PoseStack.Pose entry,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      float minU,
      float maxU,
      float minV,
      float maxV,
      int light
  ) {
    int overlay = OverlayTexture.NO_OVERLAY;
    builder
        .addVertex(entry, x1, y1, z1)
        .setColor(WATER_COLOR)
        .setUv(minU, minV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, 0.0F, 1.0F, 0.0F);
    builder
        .addVertex(entry, x2, y2, z2)
        .setColor(WATER_COLOR)
        .setUv(maxU, minV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, 0.0F, 1.0F, 0.0F);
    builder
        .addVertex(entry, x3, y3, z3)
        .setColor(WATER_COLOR)
        .setUv(maxU, maxV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, 0.0F, 1.0F, 0.0F);
    builder
        .addVertex(entry, x4, y4, z4)
        .setColor(WATER_COLOR)
        .setUv(minU, maxV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, 0.0F, 1.0F, 0.0F);
  }

  @Override
  public ResourceLocation getTextureLocation(WaterSiphonEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
