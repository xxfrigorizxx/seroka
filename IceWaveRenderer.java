package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.entity.IceWaveEntity;
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

/** Rendu 3D Vague de Glace — maillage déferlant avec texture packed_ice. */
public class IceWaveRenderer extends EntityRenderer<IceWaveEntity> {

  private static final ResourceLocation PACKED_ICE_SPRITE =
      ResourceLocation.withDefaultNamespace("block/packed_ice");
  private static final int ICE_COLOR = FastColor.ARGB32.color(0xDD, 0xE8, 0xF8, 0xFF);

  public IceWaveRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public void render(
      IceWaveEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(PACKED_ICE_SPRITE);

    float age = entity.tickCount + partialTick;
    float scroll = age * 0.05F;
    float yRot = entity.getFixedYaw();

    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));

    VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    render3DBreakerWave(poseStack, vertices, sprite, scroll, packedLight);

    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  private void render3DBreakerWave(
      PoseStack poseStack,
      VertexConsumer builder,
      TextureAtlasSprite sprite,
      float scroll,
      int light
  ) {
    PoseStack.Pose entry = poseStack.last();
    int segmentsX = 16;
    float widthArc = (float) Math.toRadians(110.0D);
    float radiusBase = 3.0F;

    float[] profileY = {0.0F, 1.1F, 2.2F, 1.8F};
    float[] profileZ = {0.0F, 0.6F, 1.2F, 1.7F};
    float[] profileT = {1.2F, 0.9F, 0.6F, 0.3F};

    int profileRings = profileY.length;

    for (int r = 0; r < profileRings - 1; r++) {
      float y1 = profileY[r];
      float y2 = profileY[r + 1];
      float zOff1 = profileZ[r];
      float zOff2 = profileZ[r + 1];
      float thick1 = profileT[r];
      float thick2 = profileT[r + 1];

      float v1 = sprite.getV(Mth.frac((float) r / profileRings + scroll));
      float v2 = sprite.getV(Mth.frac((float) (r + 1) / profileRings + scroll));

      for (int i = 0; i < segmentsX; i++) {
        float uFrac1 = (float) i / segmentsX;
        float uFrac2 = (float) (i + 1) / segmentsX;
        float angle1 = -widthArc / 2.0F + (widthArc * uFrac1);
        float angle2 = -widthArc / 2.0F + (widthArc * uFrac2);

        float u1 = sprite.getU(Mth.frac(uFrac1));
        float u2 = sprite.getU(Mth.frac(uFrac2));

        float x1F = (float) Math.sin(angle1) * radiusBase;
        float z1F = (float) Math.cos(angle1) * radiusBase + zOff1;
        float x2F = (float) Math.sin(angle2) * radiusBase;
        float z2F = (float) Math.cos(angle2) * radiusBase + zOff1;

        float x3F = (float) Math.sin(angle2) * radiusBase;
        float z3F = (float) Math.cos(angle2) * radiusBase + zOff2;
        float x4F = (float) Math.sin(angle1) * radiusBase;
        float z4F = (float) Math.cos(angle1) * radiusBase + zOff2;

        float x1B = x1F;
        float z1B = z1F - thick1;
        float x2B = x2F;
        float z2B = z2F - thick1;

        float x3B = x3F;
        float z3B = z3F - thick2;
        float x4B = x4F;
        float z4B = z4F - thick2;

        addQuad(builder, entry, x1F, y1, z1F, x2F, y1, z2F, x3F, y2, z3F, x4F, y2, z4F, u1, u2, v1, v2, light, 0.0F, 0.5F, 1.0F);
        addQuad(builder, entry, x2B, y1, z2B, x1B, y1, z1B, x4B, y2, z4B, x3B, y2, z3B, u1, u2, v1, v2, light, 0.0F, -0.5F, -1.0F);

        if (r == profileRings - 2) {
          addQuad(builder, entry, x4F, y2, z4F, x3F, y2, z3F, x3B, y2, z3B, x4B, y2, z4B, u1, u2, v1, v2, light, 0.0F, 1.0F, 0.0F);
        }

        if (i == 0) {
          addQuad(builder, entry, x1B, y1, z1B, x1F, y1, z1F, x4F, y2, z4F, x4B, y2, z4B, u1, u2, v1, v2, light, -1.0F, 0.0F, 0.0F);
        }
        if (i == segmentsX - 1) {
          addQuad(builder, entry, x2F, y1, z2F, x2B, y1, z2B, x3B, y2, z3B, x3F, y2, z3F, u1, u2, v1, v2, light, 1.0F, 0.0F, 0.0F);
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
      float nx,
      float ny,
      float nz
  ) {
    int overlay = OverlayTexture.NO_OVERLAY;
    builder
        .addVertex(entry, x1, y1, z1)
        .setColor(ICE_COLOR)
        .setUv(minU, minV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, nx, ny, nz);
    builder
        .addVertex(entry, x2, y2, z2)
        .setColor(ICE_COLOR)
        .setUv(maxU, minV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, nx, ny, nz);
    builder
        .addVertex(entry, x3, y3, z3)
        .setColor(ICE_COLOR)
        .setUv(maxU, maxV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, nx, ny, nz);
    builder
        .addVertex(entry, x4, y4, z4)
        .setColor(ICE_COLOR)
        .setUv(minU, maxV)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(entry, nx, ny, nz);
  }

  @Override
  public ResourceLocation getTextureLocation(IceWaveEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
