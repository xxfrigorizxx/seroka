package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Bénédiction du Dieu Villageois : fuite rapide face aux menaces. */
public final class VillageoisBlessingHandler {

  private static final ResourceLocation SPEED_ID = ModMain.id("villageois_speed");
  private static final double SPEED_BONUS = 1.0D;
  private static final double HOSTILE_DETECTION_RADIUS = 16.0D;

  private VillageoisBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VILLAGEOIS)) {
      return;
    }
    updateSpeedBonus(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VILLAGEOIS)) {
      return;
    }
    updateSpeedBonus(player);
  }

  public static void clear(Player player) {
    removeSpeedModifier(player);
  }

  private static void updateSpeedBonus(ServerPlayer player) {
    if (hasHostileMobNearby(player)) {
      applySpeedModifier(player);
      return;
    }
    removeSpeedModifier(player);
  }

  private static boolean hasHostileMobNearby(ServerPlayer player) {
    AABB area = player.getBoundingBox().inflate(HOSTILE_DETECTION_RADIUS);
    for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, area)) {
      if (!mob.isAlive() || mob.isSpectator()) {
        continue;
      }
      if (mob instanceof Enemy) {
        return true;
      }
    }
    return false;
  }

  private static void applySpeedModifier(ServerPlayer player) {
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
