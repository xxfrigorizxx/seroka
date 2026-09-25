package com.seroka.client.renderer;

import com.seroka.catalog.CatalogMobEntity;
import com.seroka.catalog.CatalogMobModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CatalogMobRenderer extends GeoEntityRenderer<CatalogMobEntity> {

  public CatalogMobRenderer(EntityRendererProvider.Context context) {
    super(context, new CatalogMobModel());
    this.shadowRadius = 0.6F;
  }

  @Override
  public ResourceLocation getTextureLocation(CatalogMobEntity entity) {
    return ResourceLocation.fromNamespaceAndPath(
        "seroka",
        "textures/entity/catalog/" + entity.getVariant() + ".png");
  }

  @Override
  protected float getDeathMaxRotation(CatalogMobEntity entity) {
    return 0.0F;
  }
}
