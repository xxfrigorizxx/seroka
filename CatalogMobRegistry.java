package com.seroka.catalog;

import com.seroka.ModMain;
import com.seroka.catalog.generated.CatalogMobEntries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

public final class CatalogMobRegistry {

  public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
      DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ModMain.MODID);

  public static final DeferredRegister<Item> SPAWN_EGGS =
      DeferredRegister.create(BuiltInRegistries.ITEM, ModMain.MODID);

  public static final DeferredHolder<EntityType<?>, EntityType<CatalogMobEntity>> CATALOG_MOB =
      ENTITY_TYPES.register("catalog_mob", () -> EntityType.Builder
          .of(CatalogMobEntity::new, MobCategory.CREATURE)
          .sized(2.5F, 4.0F)
          .clientTrackingRange(10)
          .build("catalog_mob"));

  private static final Map<String, CatalogMobEntries.Entry> BY_ID = new HashMap<>();
  private static final Map<String, DeferredHolder<Item, CatalogMobSpawnEggItem>> EGGS = new HashMap<>();

  static {
    for (CatalogMobEntries.Entry entry : CatalogMobEntries.ALL) {
      BY_ID.put(entry.id(), entry);
      EGGS.put(entry.id(), registerEgg(entry));
    }
  }

  private CatalogMobRegistry() {}

  private static DeferredHolder<Item, CatalogMobSpawnEggItem> registerEgg(CatalogMobEntries.Entry entry) {
    return SPAWN_EGGS.register(
        "catalog_" + entry.id() + "_spawn_egg",
        () -> new CatalogMobSpawnEggItem(entry.id(), new Item.Properties()));
  }

  public static CatalogMobEntries.Entry entryFor(String id) {
    return BY_ID.get(id);
  }

  public static Iterable<Map.Entry<String, DeferredHolder<Item, CatalogMobSpawnEggItem>>> eggs() {
    return EGGS.entrySet();
  }

  public static DeferredHolder<Item, CatalogMobSpawnEggItem> eggFor(String id) {
    return EGGS.get(id);
  }
}
