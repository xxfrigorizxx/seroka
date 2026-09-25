package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.entity.BloodTornadoEntity;
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

/** Rendu 3D unique de la Tornade de Sang — paroi fluide épaisse avec bordure supérieure scellée. */
public class BloodTornadoRenderer extends EntityRenderer<BloodTornadoEntity> {

  private static final ResourceLocation WATER_FLOW_SPRITE =
      ResourceLocation.withDefaultNamespace("block/water_flow");

  private static final int BLOOD_COLOR_OUTER = FastColor.ARGB32.color(0xEE, 0xE0, 0x08, 0x18);
  private static final int BLOOD_COLOR_INNER = FastColor.ARGB32.color(0xDD, 0x99, 0x02, 0x0A);
  private static final float WALL_THICKNESS = 0.35F;

  public BloodTornadoRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public void render(
      BloodTornadoEntity entity,
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
    float yRot = entity.getFixedYaw();

    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));

    VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    renderSingleThickFunnel(poseStack, vertices, sprite, age, packedLight);

    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  private void renderSingleThickFunnel(
      PoseStack poseStack,
      VertexConsumer builder,
      TextureAtlasSprite sprite,
      float age,
      int light
  ) {
    PoseStack.Pose entry = poseStack.last();
    int rings = 14;
    int segments = 28;
    float totalHeight = 4.5F;
    float baseRadius = 0.45F;
    float topRadius = 2.6F;

    float uScroll = age * 0.08F;
    float vScroll = age * 0.15F;
    float spinAngle = age * 28.0F;

    float halfThick = WALL_THICKNESS / 2.0F;

    for (int r = 0; r < rings - 1; r++) {
      float frac1 = (float) r / rings;
      float frac2 = (float) (r + 1) / rings;

      float y1 = frac1 * totalHeight;
      float y2 = frac2 * totalHeight;

      float wave1X = Mth.sin(age * 0.15F + frac1 * 3.0F) * 0.15F;
      float wave1Z = Mth.cos(age * 0.15F + frac1 * 3.0F) * 0.15F;
      float wave2X = Mth.sin(age * 0.15F + frac2 * 3.0F) * 0.15F;
      float wave2Z = Mth.cos(age * 0.15F + frac2 * 3.0F) * 0.15F;

      float midRadius1 = baseRadius + (frac1 * frac1) * (topRadius - baseRadius);
      float midRadius2 = baseRadius + (frac2 * frac2) * (topRadius - baseRadius);

      float r1Out = midRadius1 + halfThick;
      float r1In = Math.max(0.05F, midRadius1 - halfThick);
      float r2Out = midRadius2 + halfThick;
      float r2In = Math.max(0.05F, midRadius2 - halfThick);

      float twist1 = spinAngle + frac1 * 140.0F;
      float twist2 = spinAngle + frac2 * 140.0F;

      for (int i = 0; i < segments; i++) {
        float segFrac1 = (float) i / segments;
        float segFrac2 = (float) (i + 1) / segments;

        float angle1_1 = (float) Math.toRadians(segFrac1 * 360.0F + twist1);
        float angle1_2 = (float) Math.toRadians(segFrac2 * 360.0F + twist1);
        float angle2_1 = (float) Math.toRadians(segFrac1 * 360.0F + twist2);
        float angle2_2 = (float) Math.toRadians(segFrac2 * 360.0F + twist2);

        float u1 = sprite.getU(Mth.frac(segFrac1 + uScroll));
        float u2 = sprite.getU(Mth.frac(segFrac2 + uScroll));
        float v1 = sprite.getV(Mth.frac(frac1 + vScroll));
        float v2 = sprite.getV(Mth.frac(frac2 + vScroll));

        float x1Out = (float) Math.cos(angle1_1) * r1Out + wave1X;
        float z1Out = (float) Math.sin(angle1_1) * r1Out + wave1Z;
        float x2Out = (float) Math.cos(angle1_2) * r1Out + wave1X;
        float z2Out = (float) Math.sin(angle1_2) * r1Out + wave1Z;
        float x3Out = (float) Math.cos(angle2_2) * r2Out + wave2X;
        float z3Out = (float) Math.sin(angle2_2) * r2Out + wave2Z;
        float x4Out = (float) Math.cos(angle2_1) * r2Out + wave2X;
        float z4Out = (float) Math.sin(angle2_1) * r2Out + wave2Z;

        float x1In = (float) Math.cos(angle1_1) * r1In + wave1X;
        float z1In = (float) Math.sin(angle1_1) * r1In + wave1Z;
        float x2In = (float) Math.cos(angle1_2) * r1In + wave1X;
        float z2In = (float) Math.sin(angle1_2) * r1In + wave1Z;
        float x3In = (float) Math.cos(angle2_2) * r2In + wave2X;
        float z3In = (float) Math.sin(angle2_2) * r2In + wave2Z;
        float x4In = (float) Math.cos(angle2_1) * r2In + wave2X;
        float z4In = (float) Math.sin(angle2_1) * r2In + wave2Z;

        addQuad(builder, entry, x1Out, y1, z1Out, x2Out, y1, z2Out, x3Out, y2, z3Out, x4Out, y2, z4Out, u1, u2, v1, v2, light, BLOOD_COLOR_OUTER);
        addQuad(builder, entry, x2In, y1, z2In, x1In, y1, z1In, x4In, y2, z4In, x3In, y2, z3In, u1, u2, v1, v2, light, BLOOD_COLOR_INNER);

        if (r == rings - 2) {
          addQuad(builder, entry, x4Out, y2, z4Out, x3Out, y2, z3Out, x3In, y2, z3In, x4In, y2, z4In, u1, u2, v1, v2, light, BLOOD_COLOR_OUTER);
        }

        if (r == 0) {
          addQuad(builder, entry, x1Out, y1, z1Out, x1In, y1, z1In, x2In, y1, z2In, x2Out, y1, z2Out, u1, u2, v1, v2, light, BLOOD_COLOR_INNER);
        }
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
      int light,
      int color
  ) {
    int overlay = OverlayTexture.NO_OVERLAY;
    builder
        .addVertex(entry, x1, y1, z1)
        .setColor(color)
        .setUv(minU, minV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, 0.0F, 1.0F, 0.0F);
    builder
        .addVertex(entry, x2, y2, z2)
        .setColor(color)
        .setUv(maxU, minV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, 0.0F, 1.0F, 0.0F);
    builder
        .addVertex(entry, x3, y3, z3)
        .setColor(color)
        .setUv(maxU, maxV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, 0.0F, 1.0F, 0.0F);
    builder
        .addVertex(entry, x4, y4, z4)
        .setColor(color)
        .setUv(minU, maxV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, 0.0F, 1.0F, 0.0F);
  }

  @Override
  public ResourceLocation getTextureLocation(BloodTornadoEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
