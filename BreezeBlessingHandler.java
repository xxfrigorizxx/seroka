package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Breeze : vol, anti-chute et charge de vent. */
public final class BreezeBlessingHandler {

  private static final float HEALTH_THRESHOLD_RATIO = 0.2F;
  private static final double BREEZE_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final int WIND_CHARGE_COOLDOWN_TICKS = 10 * 20;
  private static final Map<UUID, Boolean> FLIGHT_ELIGIBLE = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> WIND_CHARGE_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private BreezeBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.BREEZE)) {
      return;
    }
    refreshIfNeeded(player);
    clearBreezeAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.BREEZE)) {
      return;
    }

    boolean eligible = hasEnoughHealth(player);
    Boolean previous = FLIGHT_ELIGIBLE.put(player.getUUID(), eligible);

    if (eligible) {
      enableFlight(player);
      if (previous != null && !previous) {
        player.displayClientMessage(
            Component.translatable("blessing.seroka.breeze.flight.restored")
                .withStyle(ChatFormatting.GREEN),
            true
        );
      }
    } else {
      disableFlight(player);
      if (previous != null && previous) {
        player.displayClientMessage(
            Component.translatable("blessing.seroka.breeze.flight.lost")
                .withStyle(ChatFormatting.RED),
            true
        );
      }
    }

    clearBreezeAggro(player);
  }

  public static void activateWindCharge(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.BREEZE)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - WIND_CHARGE_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < WIND_CHARGE_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((WIND_CHARGE_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.breeze.wind_charge.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    launchWindCharge(player);
    WIND_CHARGE_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.breeze.wind_charge.used")
            .withStyle(ChatFormatting.AQUA),
        true
    );
  }

  public static boolean isProtectedFromBreezes(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.BREEZE);
  }

  public static boolean isBreezeDamage(DamageSource source) {
    if (source.getEntity() instanceof Breeze) {
      return true;
    }
    if (source.getDirectEntity() instanceof WindCharge windCharge) {
      return windCharge.getOwner() instanceof Breeze;
    }
    return false;
  }

  public static void clear(Player player) {
    FLIGHT_ELIGIBLE.remove(player.getUUID());
    WIND_CHARGE_COOLDOWN_LAST_TICK.remove(player.getUUID());
    if (player instanceof ServerPlayer serverPlayer) {
      disableFlight(serverPlayer);
    }
  }

  public static void clearBreezeAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.BREEZE)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(BREEZE_AGGRO_CLEAR_RADIUS);
    for (Breeze breeze : player.serverLevel().getEntitiesOfClass(Breeze.class, area)) {
      if (breeze.getTarget() == player) {
        breeze.setTarget(null);
      }
      if (breeze.getLastHurtByMob() == player) {
        breeze.setLastHurtByMob(null);
      }
    }
  }

  private static void launchWindCharge(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    Vec3 look = player.getViewVector(1.0F);
    WindCharge windCharge = new WindCharge(player, level, look.x, look.y, look.z);
    windCharge.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
    level.addFreshEntity(windCharge);
  }

  private static boolean hasEnoughHealth(ServerPlayer player) {
    return player.getHealth() > player.getMaxHealth() * HEALTH_THRESHOLD_RATIO;
  }

  private static void enableFlight(ServerPlayer player) {
    ChimereFlightHelper.grantMayfly(player);
  }

  private static void disableFlight(ServerPlayer player) {
    ChimereFlightHelper.revokeMayfly(player);
  }
}
