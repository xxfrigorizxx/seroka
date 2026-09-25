package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.entity.WaterSlashEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class WaterSlashRenderer extends EntityRenderer<WaterSlashEntity> {

  private static final ResourceLocation WATER_FLOW_SPRITE =
      ResourceLocation.withDefaultNamespace("block/water_flow");
  /** Bleu eau translucide vibrant (#33A0FF avec alpha CC). */
  private static final int WATER_COLOR_R = 0x33;
  private static final int WATER_COLOR_G = 0xA0;
  private static final int WATER_COLOR_B = 0xFF;
  private static final int WATER_COLOR_A = 0xCC;

  /** Épaisseur 3D de la lame (en blocs, ~4-5 pixels de hauteur). */
  private static final float THICKNESS = 0.25F;

  public WaterSlashRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public void render(
      WaterSlashEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(WATER_FLOW_SPRITE);
    float scroll = (entity.tickCount + partialTick) * 0.05F;

    float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
    float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(yRot - 90.0F));
    poseStack.mulPose(Axis.XP.rotationDegrees(-xRot));

    VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    render3DCrescentMesh(poseStack, vertices, sprite, scroll, packedLight);

    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  private void render3DCrescentMesh(
      PoseStack poseStack,
      VertexConsumer builder,
      TextureAtlasSprite sprite,
      float scroll,
      int light
  ) {
    PoseStack.Pose entry = poseStack.last();
    int segments = 16;
    float radiusInner = 1.2F;
    float radiusOuter = 2.4F;
    float arcAngle = (float) Math.toRadians(130.0D);
    float halfH = THICKNESS / 2.0F;

    for (int i = 0; i < segments; i++) {
      float u1 = (float) i / segments;
      float u2 = (float) (i + 1) / segments;
      float angle1 = -arcAngle / 2.0F + (arcAngle * u1);
      float angle2 = -arcAngle / 2.0F + (arcAngle * u2);

      float minU1 = sprite.getU(Mth.frac(u1 + scroll));
      float maxU2 = sprite.getU(Mth.frac(u2 + scroll));
      float minV = sprite.getV0();
      float maxV = sprite.getV1();

      float x1In = (float) Math.cos(angle1) * radiusInner;
      float z1In = (float) Math.sin(angle1) * radiusInner;
      float x1Out = (float) Math.cos(angle1) * radiusOuter;
      float z1Out = (float) Math.sin(angle1) * radiusOuter;

      float x2In = (float) Math.cos(angle2) * radiusInner;
      float z2In = (float) Math.sin(angle2) * radiusInner;
      float x2Out = (float) Math.cos(angle2) * radiusOuter;
      float z2Out = (float) Math.sin(angle2) * radiusOuter;

      addQuad(
          builder, entry,
          x1In, halfH, z1In,
          x1Out, halfH, z1Out,
          x2Out, halfH, z2Out,
          x2In, halfH, z2In,
          minU1, maxU2, minV, maxV, light, 0.0F, 1.0F, 0.0F
      );

      addQuad(
          builder, entry,
          x2In, -halfH, z2In,
          x2Out, -halfH, z2Out,
          x1Out, -halfH, z1Out,
          x1In, -halfH, z1In,
          minU1, maxU2, minV, maxV, light, 0.0F, -1.0F, 0.0F
      );

      addQuad(
          builder, entry,
          x1Out, halfH, z1Out,
          x1Out, -halfH, z1Out,
          x2Out, -halfH, z2Out,
          x2Out, halfH, z2Out,
          minU1, maxU2, minV, maxV, light, x1Out, 0.0F, z1Out
      );

      addQuad(
          builder, entry,
          x2In, halfH, z2In,
          x2In, -halfH, z2In,
          x1In, -halfH, z1In,
          x1In, halfH, z1In,
          minU1, maxU2, minV, maxV, light, -x1In, 0.0F, -z1In
      );
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
    builder
        .addVertex(entry, x1, y1, z1)
        .setColor(WATER_COLOR_R, WATER_COLOR_G, WATER_COLOR_B, WATER_COLOR_A)
        .setUv(minU, minV)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(entry, nx, ny, nz);
    builder
        .addVertex(entry, x2, y2, z2)
        .setColor(WATER_COLOR_R, WATER_COLOR_G, WATER_COLOR_B, WATER_COLOR_A)
        .setUv(maxU, minV)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(entry, nx, ny, nz);
    builder
        .addVertex(entry, x3, y3, z3)
        .setColor(WATER_COLOR_R, WATER_COLOR_G, WATER_COLOR_B, WATER_COLOR_A)
        .setUv(maxU, maxV)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(entry, nx, ny, nz);
    builder
        .addVertex(entry, x4, y4, z4)
        .setColor(WATER_COLOR_R, WATER_COLOR_G, WATER_COLOR_B, WATER_COLOR_A)
        .setUv(minU, maxV)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(entry, nx, ny, nz);
  }

  @Override
  public ResourceLocation getTextureLocation(WaterSlashEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
