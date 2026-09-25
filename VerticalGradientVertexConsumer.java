package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

/** Applique un dégradé vertical d'alpha (base opaque → sommet atténué). */
final class VerticalGradientVertexConsumer implements VertexConsumer {

  private final VertexConsumer delegate;
  private final float minY;
  private final float maxY;
  private final float topAlphaFactor;
  private float pendingY;

  VerticalGradientVertexConsumer(
      VertexConsumer delegate,
      float minY,
      float maxY,
      float topAlphaFactor
  ) {
    this.delegate = delegate;
    this.minY = minY;
    this.maxY = maxY;
    this.topAlphaFactor = topAlphaFactor;
    this.pendingY = minY;
  }

  @Override
  public VertexConsumer addVertex(float x, float y, float z) {
    pendingY = y;
    return delegate.addVertex(x, y, z);
  }

  @Override
  public VertexConsumer setColor(int r, int g, int b, int a) {
    int scaledA = scaleAlpha(a, pendingY);
    return delegate.setColor(r, g, b, scaledA);
  }

  @Override
  public VertexConsumer setColor(int packedColor) {
    return delegate.setColor(applyGradient(packedColor, pendingY));
  }

  @Override
  public VertexConsumer setUv(float u, float v) {
    return delegate.setUv(u, v);
  }

  @Override
  public VertexConsumer setUv1(int u, int v) {
    return delegate.setUv1(u, v);
  }

  @Override
  public VertexConsumer setUv2(int u, int v) {
    return delegate.setUv2(u, v);
  }

  @Override
  public VertexConsumer setOverlay(int overlayCoords) {
    return delegate.setOverlay(overlayCoords);
  }

  @Override
  public VertexConsumer setLight(int packedLight) {
    return delegate.setLight(packedLight);
  }

  @Override
  public VertexConsumer setNormal(float x, float y, float z) {
    return delegate.setNormal(x, y, z);
  }

  @Override
  public void addVertex(
      float x,
      float y,
      float z,
      int color,
      float u,
      float v,
      int overlay,
      int light,
      float normalX,
      float normalY,
      float normalZ
  ) {
    delegate.addVertex(
        x,
        y,
        z,
        applyGradient(color, y),
        u,
        v,
        overlay,
        light,
        normalX,
        normalY,
        normalZ
    );
  }

  private int applyGradient(int color, float y) {
    int a = (color >> 24) & 0xFF;
    if (a == 0) {
      a = 0xFF;
    }
    int scaledA = scaleAlpha(a, y);
    return (scaledA << 24) | (color & 0x00FFFFFF);
  }

  private int scaleAlpha(int alpha, float y) {
    float span = maxY - minY;
    float t = span <= 1.0E-4F ? 0.0F : Mth.clamp((y - minY) / span, 0.0F, 1.0F);
    float factor = 1.0F - t * (1.0F - topAlphaFactor);
    return Mth.clamp((int) (alpha * factor), 0, 255);
  }
}
