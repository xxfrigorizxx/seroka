package com.seroka.catalog.generated;

/** Généré par tools/import_mob_catalog.py — ne pas éditer à la main. */
public final class CatalogMobEntries {

  public record Entry(
      String id,
      String displayName,
      String license,
      String source,
      float width,
      float height,
      float eyeHeight,
      String idleAnim,
      String walkAnim,
      String attackAnim,
      int eggColor1,
      int eggColor2
  ) {}

  public static final Entry[] ALL = {
    new Entry("gl_parasite", "Parasite", "MIT", "GeckoLib Examples", 1.5f, 3.5f, 1.25f, "misc.idle", "move.walk", "attack.strike", 0x3E560C, 0x0D3E56),
    new Entry("gl_gremlin", "Gremlin", "MIT", "GeckoLib Examples", 2.0f, 3.5f, 1.25f, "misc.idle", "move.walk", "attack.block.left", 0x433813, 0x394338),
    new Entry("gl_mutant_zombie", "Mutant Zombie", "MIT", "GeckoLib Examples", 2.0f, 3.5f, 1.25f, "misc.idle", "misc.idle", "attack.block.left", 0x76CEFF, 0x5576CE),
    new Entry("gl_bat", "Bat", "MIT", "GeckoLib Examples", 2.0f, 4.0f, 1.0f, "misc.idle", "move.walk", "move.fly", 0x2C6E2A, 0xC22C6E),
    new Entry("gl_cool_kid", "Cool Kid", "MIT", "GeckoLib Examples", 1.5f, 2.5f, 0.8f, "misc.idle", "move.walk", "misc.idle", 0x97093C, 0x189709),
    new Entry("az_juravenator", "Juravenator", "MIT", "Azurelib Examples", 6.0f, 4.5f, 1.75f, "idle", "walk", "walk", 0x7E4B07, 0x747E4B),
    new Entry("az_manul", "Manul", "MIT", "Azurelib Examples", 2.0f, 2.5f, 0.8f, "Idle", "run", "Idle", 0xB41C35, 0x39B41C),
  };

  private CatalogMobEntries() {}
}
