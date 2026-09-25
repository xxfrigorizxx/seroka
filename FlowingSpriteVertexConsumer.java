package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/** Remappe les UV du modèle vers un sprite d'atlas (eau animée) avec défilement. */
public final class FlowingSpriteVertexConsumer implements VertexConsumer {

  private final VertexConsumer delegate;
  private final TextureAtlasSprite sprite;
  private final float uScroll;
  private final float vScroll;

  public FlowingSpriteVertexConsumer(
      VertexConsumer delegate,
      TextureAtlasSprite sprite,
      float uScroll,
      float vScroll
  ) {
    this.delegate = delegate;
    this.sprite = sprite;
    this.uScroll = uScroll;
    this.vScroll = vScroll;
  }

  @Override
  public VertexConsumer addVertex(float x, float y, float z) {
    return delegate.addVertex(x, y, z);
  }

  @Override
  public VertexConsumer setColor(int r, int g, int b, int a) {
    return delegate.setColor(r, g, b, a);
  }

  @Override
  public VertexConsumer setColor(int packedColor) {
    return delegate.setColor(packedColor);
  }

  @Override
  public VertexConsumer setUv(float u, float v) {
    return delegate.setUv(mapU(u), mapV(v));
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
    delegate.addVertex(x, y, z, color, mapU(u), mapV(v), overlay, light, normalX, normalY, normalZ);
  }

  private float mapU(float u) {
    return sprite.getU(Mth.frac(u * 2.0F + uScroll));
  }

  private float mapV(float v) {
    return sprite.getV(Mth.frac(v * 2.0F + vScroll));
  }
}
