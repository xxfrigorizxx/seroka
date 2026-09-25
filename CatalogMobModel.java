package com.seroka.catalog;

import com.seroka.ModMain;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CatalogMobModel extends GeoModel<CatalogMobEntity> {

  @Override
  public ResourceLocation getModelResource(CatalogMobEntity entity) {
    return ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "geo/catalog/" + entity.getVariant() + ".geo.json");
  }

  @Override
  public ResourceLocation getTextureResource(CatalogMobEntity entity) {
    return ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "textures/entity/catalog/" + entity.getVariant() + ".png");
  }

  @Override
  public ResourceLocation getAnimationResource(CatalogMobEntity entity) {
    return ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "animations/catalog/" + entity.getVariant() + ".animation.json");
  }
}
