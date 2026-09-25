package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Perroquet : vol lent tant que les PV restent au-dessus de 50 %. */
public final class PerroquetBlessingHandler {

  private static final ResourceLocation FLYING_SPEED_ID = ModMain.id("perroquet_flying_speed");
  private static final double FLYING_SPEED_MULTIPLIER = -0.9D;
  private static final float HEALTH_THRESHOLD_RATIO = 0.5F;
  private static final Map<UUID, Boolean> FLIGHT_ELIGIBLE = new ConcurrentHashMap<>();

  private PerroquetBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PERROQUET)) {
      return;
    }
    applyFlyingSpeedModifier(player);
    refreshIfNeeded(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PERROQUET)) {
      return;
    }

    applyFlyingSpeedModifier(player);

    boolean eligible = hasEnoughHealth(player);
    Boolean previous = FLIGHT_ELIGIBLE.put(player.getUUID(), eligible);

    if (eligible) {
      enableFlight(player);
      if (previous != null && !previous) {
        player.displayClientMessage(
            Component.translatable("blessing.seroka.perroquet.flight.restored")
                .withStyle(ChatFormatting.GREEN),
            true
        );
      }
      return;
    }

    disableFlight(player);
    if (previous != null && previous) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.perroquet.flight.lost")
              .withStyle(ChatFormatting.RED),
          true
      );
    }
  }

  public static void clear(Player player) {
    FLIGHT_ELIGIBLE.remove(player.getUUID());
    removeFlyingSpeedModifier(player);
    if (player instanceof ServerPlayer serverPlayer) {
      disableFlight(serverPlayer);
    }
  }

  private static boolean hasEnoughHealth(ServerPlayer player) {
    return player.getHealth() > player.getMaxHealth() * HEALTH_THRESHOLD_RATIO;
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
