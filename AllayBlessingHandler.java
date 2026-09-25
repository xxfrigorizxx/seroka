package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Bénédiction du Dieu Allay : vol lent permanent. */
public final class AllayBlessingHandler {

  private static final ResourceLocation FLYING_SPEED_ID = ModMain.id("allay_flying_speed");
  private static final double FLYING_SPEED_MULTIPLIER = -0.5D;

  private AllayBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ALLAY)) {
      return;
    }
    applyFlyingSpeedModifier(player);
    enableFlight(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ALLAY)) {
      return;
    }
    applyFlyingSpeedModifier(player);
    enableFlight(player);
  }

  public static void clear(Player player) {
    removeFlyingSpeedModifier(player);
    if (player instanceof ServerPlayer serverPlayer) {
      disableFlight(serverPlayer);
    }
  }

  private static void applyFlyingSpeedModifier(ServerPlayer player) {
    AttributeInstance instance = player.getAttribute(Attributes.FLYING_SPEED);
    if (instance == null || instance.getModifier(FLYING_SPEED_ID) != null) {
      return;
    }
    instance.addPermanentModifier(
        new AttributeModifier(FLYING_SPEED_ID, FLYING_SPEED_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );
  }

  private static void removeFlyingSpeedModifier(Player player) {
    AttributeInstance instance = player.getAttribute(Attributes.FLYING_SPEED);
    if (instance != null) {
      instance.removeModifier(FLYING_SPEED_ID);
    }
  }

  private static void enableFlight(ServerPlayer player) {
    ChimereFlightHelper.grantMayfly(player);
  }

  private static void disableFlight(ServerPlayer player) {
    ChimereFlightHelper.revokeMayfly(player);
  }
}
