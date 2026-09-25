package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Blaze : vol, résistance thermique et boules de feu. */
public final class BlazeBlessingHandler {

  private static final float HEALTH_THRESHOLD_RATIO = 0.8F;
  private static final double BLAZE_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final int FIREBALL_COOLDOWN_TICKS = 5 * 20;
  private static final Map<UUID, Boolean> FLIGHT_ELIGIBLE = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> FIREBALL_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private BlazeBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.BLAZE)) {
      return;
    }
    refreshIfNeeded(player);
    extinguishIfBurning(player);
    clearBlazeAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.BLAZE)) {
      return;
    }

    boolean eligible = hasEnoughHealth(player);
    Boolean previous = FLIGHT_ELIGIBLE.put(player.getUUID(), eligible);

    if (eligible) {
      enableFlight(player);
      if (previous != null && !previous) {
        player.displayClientMessage(
            Component.translatable("blessing.seroka.blaze.flight.restored")
                .withStyle(ChatFormatting.GREEN),
            true
        );
      }
    } else {
      disableFlight(player);
      if (previous != null && previous) {
        player.displayClientMessage(
            Component.translatable("blessing.seroka.blaze.flight.lost")
                .withStyle(ChatFormatting.RED),
            true
        );
      }
    }

    extinguishIfBurning(player);
    clearBlazeAggro(player);
  }

  public static void activateFireball(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.BLAZE)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - FIREBALL_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < FIREBALL_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((FIREBALL_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.blaze.fireball.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    launchFireball(player);
    FIREBALL_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.blaze.fireball.used")
            .withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static boolean isProtectedFromBlazes(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.BLAZE);
  }

  public static boolean isFireOrLavaDamage(DamageSource source) {
    return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA);
  }

  public static boolean isBlazeProjectile(DamageSource source) {
    if (source.getDirectEntity() instanceof SmallFireball fireball) {
      return fireball.getOwner() instanceof Blaze;
    }
    return source.getEntity() instanceof Blaze;
  }

  public static void clear(Player player) {
    FLIGHT_ELIGIBLE.remove(player.getUUID());
    FIREBALL_COOLDOWN_LAST_TICK.remove(player.getUUID());
    if (player instanceof ServerPlayer serverPlayer) {
      disableFlight(serverPlayer);
    }
  }

  public static void clearBlazeAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.BLAZE)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(BLAZE_AGGRO_CLEAR_RADIUS);
    for (Blaze blaze : player.serverLevel().getEntitiesOfClass(Blaze.class, area)) {
      if (blaze.getTarget() == player) {
        blaze.setTarget(null);
      }
      if (blaze.getLastHurtByMob() == player) {
        blaze.setLastHurtByMob(null);
      }
    }
  }

  private static void launchFireball(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    Vec3 look = player.getViewVector(1.0F);
    SmallFireball fireball = new SmallFireball(level, player, look);
    fireball.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
    level.addFreshEntity(fireball);
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

  private static void extinguishIfBurning(ServerPlayer player) {
    if (player.isOnFire()) {
      player.clearFire();
    }
  }
}
