package com.seroka.client.organic;

import com.seroka.organic.IEvolvingMob;
import com.seroka.organic.MobEvolution;
import com.seroka.organic.OrganicEvolutionDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/** Vue client des mobs gouvernés par {@link IEvolvingMob}. */
public final class OrganicEvolutionClient {

  private OrganicEvolutionClient() {}

  public static boolean isEvolvingMob(Entity entity) {
    return entity instanceof Mob mob && OrganicEvolutionDisplay.showsLevelTag(mob);
  }

  public static IEvolvingMob asEvolvingMob(Entity entity) {
    if (!(entity instanceof Mob mob) || !isEvolvingMob(mob)) {
      return null;
    }
    return MobEvolution.of(mob);
  }
}
