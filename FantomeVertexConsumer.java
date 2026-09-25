package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

/**
 * Rend translucide tout ce qui passe par là : les planches manquantes d'une coque se montrent en
 * fantômes, comme un plan posé sur le trou.
 */
final class FantomeVertexConsumer implements VertexConsumer {

  private final VertexConsumer delegue;
  private final float opacite;

  FantomeVertexConsumer(VertexConsumer delegue, float opacite) {
    this.delegue = delegue;
    this.opacite = opacite;
  }

  @Override
  public VertexConsumer addVertex(float x, float y, float z) {
    return delegue.addVertex(x, y, z);
  }

  @Override
  public VertexConsumer setColor(int r, int g, int b, int a) {
    return delegue.setColor(r, g, b, attenuer(a));
  }

  @Override
  public VertexConsumer setColor(int couleurEmpaquetee) {
    return delegue.setColor(attenuerEmpaquetee(couleurEmpaquetee));
  }

  @Override
  public VertexConsumer setUv(float u, float v) {
    return delegue.setUv(u, v);
  }

  @Override
  public VertexConsumer setUv1(int u, int v) {
    return delegue.setUv1(u, v);
  }

  @Override
  public VertexConsumer setUv2(int u, int v) {
    return delegue.setUv2(u, v);
  }

  @Override
  public VertexConsumer setOverlay(int recouvrement) {
    return delegue.setOverlay(recouvrement);
  }

  @Override
  public VertexConsumer setLight(int lumiere) {
    return delegue.setLight(lumiere);
  }

  @Override
  public VertexConsumer setNormal(float x, float y, float z) {
    return delegue.setNormal(x, y, z);
  }

  @Override
  public void addVertex(float x, float y, float z, int couleur, float u, float v, int recouvrement,
      int lumiere, float normaleX, float normaleY, float normaleZ) {
    delegue.addVertex(x, y, z, attenuerEmpaquetee(couleur), u, v, recouvrement, lumiere,
        normaleX, normaleY, normaleZ);
  }

  private int attenuerEmpaquetee(int couleur) {
    int alpha = (couleur >> 24) & 0xFF;
    if (alpha == 0) {
      alpha = 0xFF;
    }
    return (attenuer(alpha) << 24) | (couleur & 0x00FFFFFF);
  }

  private int attenuer(int alpha) {
    return Mth.clamp((int) (alpha * opacite), 0, 255);
  }
}
