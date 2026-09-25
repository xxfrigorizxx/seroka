package com.seroka.client;

import com.seroka.client.renderer.EssenceEauItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Extensions client pour l'essence d'eau — rendu 3D via BEWLR. */
public final class EssenceEauClientExtensions implements IClientItemExtensions {

  public static final EssenceEauClientExtensions INSTANCE = new EssenceEauClientExtensions();

  private EssenceEauItemRenderer renderer;

  private EssenceEauClientExtensions() {}

  public void bindRenderer(EssenceEauItemRenderer renderer) {
    this.renderer = renderer;
  }

  @Override
  public BlockEntityWithoutLevelRenderer getCustomRenderer() {
    if (renderer == null) {
      renderer = EssenceEauItemRenderer.getInstance();
    }
    return renderer;
  }
}
