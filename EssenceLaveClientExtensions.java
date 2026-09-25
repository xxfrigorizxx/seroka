package com.seroka.client;

import com.seroka.client.renderer.EssenceLaveItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Extensions client pour l'essence de lave — rendu 3D via BEWLR. */
public final class EssenceLaveClientExtensions implements IClientItemExtensions {

  public static final EssenceLaveClientExtensions INSTANCE = new EssenceLaveClientExtensions();

  private EssenceLaveItemRenderer renderer;

  private EssenceLaveClientExtensions() {}

  public void bindRenderer(EssenceLaveItemRenderer renderer) {
    this.renderer = renderer;
  }

  @Override
  public BlockEntityWithoutLevelRenderer getCustomRenderer() {
    if (renderer == null) {
      renderer = EssenceLaveItemRenderer.getInstance();
    }
    return renderer;
  }
}
