package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Endermite : téléportation et neutralité. */
public final class EndermiteBlessingHandler {

  private static final double TELEPORT_MAX_DISTANCE = 15.0D;
  private static final int TELEPORT_COOLDOWN_TICKS = 60 * 20;
  private static final double ENDERMITE_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final Map<UUID, Long> TELEPORT_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private EndermiteBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDERMITE)) {
      return;
    }
    clearEndermiteAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDERMITE)) {
      return;
    }
    clearEndermiteAggro(player);
  }

  public static boolean isProtectedFromEndermites(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.ENDERMITE);
  }

  public static boolean isEndermiteDamage(DamageSource source) {
    return source.getEntity() instanceof Endermite || source.getDirectEntity() instanceof Endermite;
  }

  public static void clearEndermiteAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDERMITE)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(ENDERMITE_AGGRO_CLEAR_RADIUS);
    for (Endermite endermite : player.serverLevel().getEntitiesOfClass(Endermite.class, area)) {
      if (endermite.getTarget() == player) {
        endermite.setTarget(null);
      }
      if (endermite.getLastHurtByMob() == player) {
        endermite.setLastHurtByMob(null);
      }
    }
  }

  public static void activateTeleport(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDERMITE)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - TELEPORT_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < TELEPORT_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((TELEPORT_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.endermite.teleport.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    Vec3 destination = resolveTeleportDestination(player);
    if (destination == null) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.endermite.teleport.blocked").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    Vec3 origin = player.position();
    TELEPORT_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    spawnTeleportParticles(player.serverLevel(), origin);
    player.teleportTo(destination.x, destination.y, destination.z);
    player.setDeltaMovement(Vec3.ZERO);
    player.fallDistance = 0.0F;
    player.hurtMarked = true;
    spawnTeleportParticles(player.serverLevel(), destination);
    player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.endermite.teleport.used").withStyle(ChatFormatting.DARK_PURPLE),
        true
    );
  }

  public static void clear(Player player) {
    TELEPORT_COOLDOWN_LAST_TICK.remove(player.getUUID());
  }

  private static Vec3 resolveTeleportDestination(ServerPlayer player) {
    Vec3 start = player.position();
    Vec3 look = player.getLookAngle();
    if (look.lengthSqr() < 1.0E-8D) {
      return null;
    }
    look = look.normalize();

    Vec3 destination = start;
    int steps = (int) Math.floor(TELEPORT_MAX_DISTANCE);
    for (int step = 0; step < steps; step++) {
      Vec3 next = destination.add(look);
      if (!canFitAt(player, next)) {
        break;
      }
      destination = next;
    }

    if (destination.distanceToSqr(start) < 1.0E-4D) {
      return null;
    }
    return destination;
  }

  private static boolean canFitAt(ServerPlayer player, Vec3 destination) {
    Vec3 delta = destination.subtract(player.position());
    return player.level().noCollision(player, player.getBoundingBox().move(delta));
  }

  private static void spawnTeleportParticles(ServerLevel level, Vec3 position) {
    level.sendParticles(
        ParticleTypes.PORTAL,
        position.x,
        position.y + 1.0D,
        position.z,
        16,
        0.35D,
        0.35D,
        0.35D,
        0.1D
    );
  }
}
