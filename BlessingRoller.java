package com.seroka.chimere;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Tirage pondéré par les places restantes de chaque dieu éligible.
 * Un dieu avec plus de places libres a proportionnellement plus de chances d'être tiré.
 */
public final class BlessingRoller {

  private BlessingRoller() {}

  public static Optional<GodDefinition> roll(RandomSource random, GodBlessingRegistry registry) {
    return roll(random, registry, Set.of());
  }

  public static Optional<GodDefinition> roll(
      RandomSource random,
      GodBlessingRegistry registry,
      Set<String> excludedGodIds
  ) {
    List<GodDefinition> eligible = new ArrayList<>();
    int totalWeight = 0;
    for (GodDefinition god : GodCatalog.ALL) {
      if (excludedGodIds.contains(god.id())) {
        continue;
      }
      if (GodIds.ENDER_DRAGON.equals(god.id()) && !EnderDragonBlessingUnlock.canRoll(registry)) {
        continue;
      }
      if (!registry.hasCapacity(god)) {
        continue;
      }
      int weight = remainingCapacity(god, registry);
      if (weight <= 0) {
        continue;
      }
      eligible.add(god);
      totalWeight += weight;
    }
    if (eligible.isEmpty() || totalWeight <= 0) {
      return Optional.empty();
    }

    int roll = random.nextInt(totalWeight);
    int cumulative = 0;
    for (GodDefinition god : eligible) {
      cumulative += remainingCapacity(god, registry);
      if (roll < cumulative) {
        return Optional.of(god);
      }
    }
    return Optional.of(eligible.getLast());
  }

  public static int totalEligibleWeight(GodBlessingRegistry registry, Set<String> excludedGodIds) {
    int totalWeight = 0;
    for (GodDefinition god : GodCatalog.ALL) {
      if (excludedGodIds.contains(god.id())) {
        continue;
      }
      if (GodIds.ENDER_DRAGON.equals(god.id()) && !EnderDragonBlessingUnlock.canRoll(registry)) {
        continue;
      }
      if (!registry.hasCapacity(god)) {
        continue;
      }
      totalWeight += remainingCapacity(god, registry);
    }
    return totalWeight;
  }

  public static int remainingCapacity(GodDefinition god, GodBlessingRegistry registry) {
    return god.maxBlessings() - registry.getActiveCount(god.id());
  }
}
