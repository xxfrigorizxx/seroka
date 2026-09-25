package com.seroka.client;

import com.seroka.client.renderer.EssenceGivrerItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Extensions client pour l'essence givrée — rendu 3D via BEWLR. */
public final class EssenceGivrerClientExtensions implements IClientItemExtensions {

  public static final EssenceGivrerClientExtensions INSTANCE = new EssenceGivrerClientExtensions();

  private EssenceGivrerItemRenderer renderer;

  private EssenceGivrerClientExtensions() {}

  public void bindRenderer(EssenceGivrerItemRenderer renderer) {
    this.renderer = renderer;
  }

  @Override
  public BlockEntityWithoutLevelRenderer getCustomRenderer() {
    if (renderer == null) {
      renderer = EssenceGivrerItemRenderer.getInstance();
    }
    return renderer;
  }
}
