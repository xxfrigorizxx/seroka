package com.seroka.chimere;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Bénédiction du Dieu Arpenteur : marche sur la lave (échafaudage) et immunité thermique. */
public final class ArpenteurBlessingHandler {

  public static final double LAVA_JUMP_VELOCITY = 0.42D;

  private static final int LAVA_SCAN_DEPTH = 4;
  private static final int LAVA_SCAN_HEIGHT = 2;
  private static final double SURFACE_EPSILON = 0.05D;
  private static final double SURFACE_SNAP_RANGE = 0.35D;

  private ArpenteurBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ARPENTEUR)) {
      return;
    }
    extinguishIfBurning(player);
  }

  public static void refreshIfNeeded(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ARPENTEUR)) {
      return;
    }
    if (player instanceof ServerPlayer serverPlayer) {
      extinguishIfBurning(serverPlayer);
    }
  }

  /** Marche à la surface autorisée (pas accroupi). */
  public static boolean shouldWalkOnLava(Player player) {
    return ChimereBlessingService.hasBlessing(player, GodIds.ARPENTEUR) && !player.isCrouching();
  }

  /** Peut se tenir sur la lave comme sur un échafaudage. */
  public static boolean canStandOnLava(Player player) {
    return shouldWalkOnLava(player) && touchesLava(player);
  }

  public static boolean isThermalDamage(DamageSource source) {
    return source.is(DamageTypes.LAVA)
        || source.is(DamageTypes.IN_FIRE)
        || source.is(DamageTypes.HOT_FLOOR);
  }

  public static void clear(ServerPlayer player) {
    // Aucun état persistant.
  }

  /**
   * Avant {@code travel()} : surface de lave, annulation de la chute, sol virtuel pour le saut.
   *
   * @return true si le joueur est maintenu à la surface de la lave
   */
  public static boolean prepareLavaSurfaceBeforeTravel(Player player) {
    if (!canStandOnLava(player)) {
      return false;
    }

    Level level = player.level();
    Optional<Double> surfaceY = findLavaSurface(level, player.getX(), player.getY(), player.getZ());
    if (surfaceY.isEmpty()) {
      return false;
    }

    double targetY = surfaceY.get();
    if (shouldSnapToLavaSurface(player, targetY)) {
      player.setPos(player.getX(), targetY, player.getZ());
    }

    Vec3 motion = player.getDeltaMovement();
    if (motion.y < 0.0D) {
      player.setDeltaMovement(motion.x, 0.0D, motion.z);
    }

    player.setSwimming(false);
    player.setOnGround(true);
    player.resetFallDistance();
    return true;
  }

  /** Impulsion de saut vanilla sur la surface de lave. */
  public static void applyLavaJump(Player player) {
    if (!canStandOnLava(player) || !player.onGround()) {
      return;
    }
    Vec3 motion = player.getDeltaMovement();
    player.setDeltaMovement(motion.x, LAVA_JUMP_VELOCITY, motion.z);
    player.setOnGround(false);
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.hurtMarked = true;
    }
  }

  public static boolean touchesLava(Player player) {
    if (player.isInLava()) {
      return true;
    }
    return findLavaSurface(player.level(), player.getX(), player.getY(), player.getZ()).isPresent();
  }

  private static boolean shouldSnapToLavaSurface(Player player, double targetY) {
    if (player.isInLava()) {
      return true;
    }
    double deltaY = player.getY() - targetY;
    return deltaY < -SURFACE_EPSILON || deltaY <= SURFACE_SNAP_RANGE;
  }

  private static Optional<Double> findLavaSurface(Level level, double x, double y, double z) {
    int centerY = Mth.floor(y);
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    for (int offset = 0; offset <= LAVA_SCAN_HEIGHT; offset++) {
      Optional<Double> surface = scanLavaColumn(level, pos, x, z, centerY + offset);
      if (surface.isPresent()) {
        return surface;
      }
    }
    for (int offset = 1; offset <= LAVA_SCAN_DEPTH; offset++) {
      Optional<Double> surface = scanLavaColumn(level, pos, x, z, centerY - offset);
      if (surface.isPresent()) {
        return surface;
      }
    }
    return Optional.empty();
  }

  private static Optional<Double> scanLavaColumn(
      Level level,
      BlockPos.MutableBlockPos pos,
      double x,
      double z,
      int y
  ) {
    pos.set(Mth.floor(x), y, Mth.floor(z));
    FluidState fluid = level.getFluidState(pos);
    if (!fluid.is(Fluids.LAVA)) {
      return Optional.empty();
    }
    return Optional.of((double) pos.getY() + fluid.getHeight(level, pos));
  }

  private static void extinguishIfBurning(ServerPlayer player) {
    if (player.isOnFire()) {
      player.clearFire();
    }
  }
}
