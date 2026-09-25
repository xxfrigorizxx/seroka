package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;

/** Bénédiction du Dieu Zombie Villageois : comme le Zombie, plus rapide près des mobs passifs. */
public final class ZombieVillageoisBlessingHandler {

  private static final ResourceLocation SPEED_ID = ModMain.id("zombie_villageois_speed");
  private static final double SPEED_BONUS = 0.1D;
  private static final double PASSIVE_MOB_DETECTION_RADIUS = 16.0D;

  private ZombieVillageoisBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ZOMBIE_VILLAGEOIS)) {
      return;
    }
    ZombieBlessingHandler.applyPassive(player);
    updateSpeedBonus(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ZOMBIE_VILLAGEOIS)) {
      return;
    }
    ZombieBlessingHandler.refreshIfNeeded(player);
    updateSpeedBonus(player);
    if (player.tickCount % 40 == 0) {
      ZombieBlessingHandler.clearZombieAggro(player);
    }
  }

  public static void clear(Player player) {
    removeSpeedModifier(player);
  }

  private static void updateSpeedBonus(ServerPlayer player) {
    if (hasPassiveMobNearby(player)) {
      applySpeedModifier(player);
      return;
    }
    removeSpeedModifier(player);
  }

  private static boolean hasPassiveMobNearby(ServerPlayer player) {
    AABB area = player.getBoundingBox().inflate(PASSIVE_MOB_DETECTION_RADIUS);
    for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, area)) {
      if (!mob.isAlive() || mob.isSpectator()) {
        continue;
      }
      if (mob instanceof Enemy) {
        continue;
      }
      if (mob.getType().getCategory().isFriendly()) {
        return true;
      }
    }
    return false;
  }

  private static void applySpeedModifier(Player player) {
    AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (instance == null || instance.getModifier(SPEED_ID) != null) {
      return;
    }
    instance.addPermanentModifier(
        new AttributeModifier(SPEED_ID, SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );
  }

  private static void removeSpeedModifier(Player player) {
    AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (instance != null) {
      instance.removeModifier(SPEED_ID);
    }
  }
}
