package com.seroka.chimere;

import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Chauve-souris : vol libre et vision nocturne. */
public final class ChauveSourisBlessingHandler {

  private static final int NIGHT_VISION_DURATION_TICKS = 60 * 20;
  private static final int NIGHT_VISION_COOLDOWN_TICKS = 90 * 20;
  private static final Map<UUID, Long> NIGHT_VISION_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private ChauveSourisBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CHAUVE_SOURIS)) {
      return;
    }
    refreshIfNeeded(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CHAUVE_SOURIS)) {
      return;
    }
    if (isFlightLocked(player)) {
      if (isFullyHealed(player)) {
        unlockFlight(player);
        player.displayClientMessage(
            Component.translatable("blessing.seroka.chauve_souris.flight.restored")
                .withStyle(ChatFormatting.GREEN),
            true
        );
        enableFlight(player);
      } else {
        suspendFlight(player);
        return;
      }
    }
    enableFlight(player);
  }

  public static void onDamageTaken(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CHAUVE_SOURIS)) {
      return;
    }
    if (isFlightLocked(player)) {
      suspendFlight(player);
      return;
    }
    lockFlight(player);
    suspendFlight(player);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.chauve_souris.flight.lost")
            .withStyle(ChatFormatting.RED),
        true
    );
  }

  public static void activateNightVision(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CHAUVE_SOURIS)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - NIGHT_VISION_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < NIGHT_VISION_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((NIGHT_VISION_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.chauve_souris.vision.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    NIGHT_VISION_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.addEffect(new MobEffectInstance(
        MobEffects.NIGHT_VISION,
        NIGHT_VISION_DURATION_TICKS,
        0,
        false,
        true,
        true
    ));
    player.playNotifySound(SoundEvents.BAT_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.chauve_souris.vision.used")
            .withStyle(ChatFormatting.DARK_PURPLE),
        true
    );
  }

  public static void clear(ServerPlayer player) {
    unlockFlight(player);
    NIGHT_VISION_COOLDOWN_LAST_TICK.remove(player.getUUID());
    disableFlight(player);
  }

  public static boolean isFlightAllowed(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CHAUVE_SOURIS)) {
      return false;
    }
    if (isFlightLocked(player) && !isFullyHealed(player)) {
      return false;
    }
    return true;
  }

  private static boolean isFlightLocked(Player player) {
    return player.getData(ModAttachments.CHAUVE_SOURIS_FLIGHT_LOCKED);
  }

  private static void lockFlight(ServerPlayer player) {
    player.setData(ModAttachments.CHAUVE_SOURIS_FLIGHT_LOCKED, true);
  }

  private static void unlockFlight(ServerPlayer player) {
    player.setData(ModAttachments.CHAUVE_SOURIS_FLIGHT_LOCKED, false);
    ChimereFlightHelper.clearSafeLanding(player);
  }

  private static boolean isFullyHealed(Player player) {
    return player.getHealth() >= player.getMaxHealth();
  }

  /**
   * Coupe le vol actif. En l'air, conserve mayfly jusqu'à l'atterrissage (pas de chute brutale).
   */
  private static void suspendFlight(ServerPlayer player) {
    if (player.getAbilities().flying) {
      player.getAbilities().flying = false;
      ChimereFlightHelper.markSafeLanding(player);
      player.onUpdateAbilities();
    }
    if (player.onGround() || !ChimereFlightHelper.expectsSafeLanding(player)) {
      disableFlight(player);
      ChimereFlightHelper.clearSafeLanding(player);
    }
  }

  private static void enableFlight(ServerPlayer player) {
    ChimereFlightHelper.grantMayfly(player);
  }

  private static void disableFlight(ServerPlayer player) {
    ChimereFlightHelper.revokeMayfly(player);
  }
}
