package com.seroka.catalog;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class CatalogMobEvents {

  private CatalogMobEvents() {}

  public static void registerAttributes(EntityAttributeCreationEvent event) {
    event.put(CatalogMobRegistry.CATALOG_MOB.get(), CatalogMobEntity.createAttributes().build());
  }
}
