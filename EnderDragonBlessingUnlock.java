package com.seroka.chimere;

/**
 * Le dieu Ender Dragon n'entre dans la roue qu'après {@value #REQUIRED_ROLLS} tirages Chimère
 * sur le serveur, sauf s'il est le seul dieu encore éligible.
 */
public final class EnderDragonBlessingUnlock {

  public static final int REQUIRED_ROLLS = 500;

  private EnderDragonBlessingUnlock() {}

  public static boolean canRoll(GodBlessingRegistry registry) {
    if (registry.getTotalChimereRolls() >= REQUIRED_ROLLS) {
      return true;
    }
    return isOnlyEligibleGod(registry);
  }

  private static boolean isOnlyEligibleGod(GodBlessingRegistry registry) {
    GodDefinition enderDragon = GodCatalog.findById(GodIds.ENDER_DRAGON);
    if (enderDragon == null || !registry.hasCapacity(enderDragon)) {
      return false;
    }

    int otherEligibleWeight = 0;
    for (GodDefinition god : GodCatalog.ALL) {
      if (GodIds.ENDER_DRAGON.equals(god.id())) {
        continue;
      }
      if (!registry.hasCapacity(god)) {
        continue;
      }
      otherEligibleWeight += BlessingRoller.remainingCapacity(god, registry);
    }
    return otherEligibleWeight <= 0;
  }
}
