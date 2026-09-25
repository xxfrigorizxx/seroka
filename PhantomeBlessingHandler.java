package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Phantôme : planeur naturel et dégâts liés à la vitesse. */
public final class PhantomeBlessingHandler {

  private static final double PHANTOM_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final double MAX_SPEED_DAMAGE_BONUS = 2.0D;
  private static final double SPEED_DAMAGE_FACTOR = 0.2D;

  private static final Map<UUID, Boolean> GLIDE_ACTIVE = new ConcurrentHashMap<>();

  private PhantomeBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PHANTOME)) {
      return;
    }
    clearPhantomAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PHANTOME)) {
      return;
    }
    tickGlideState(player);
    clearPhantomAggro(player);
  }

  /** Élytre simulée uniquement pendant le planeur activé (clic molette). */
  public static boolean isPhantomElytraActive(Player player) {
    return ChimereBlessingService.hasBlessing(player, GodIds.PHANTOME)
        && Boolean.TRUE.equals(GLIDE_ACTIVE.get(player.getUUID()));
  }

  public static void toggleGlide(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PHANTOME)) {
      return;
    }

    if (player.isFallFlying()) {
      stopGliding(player);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.phantome.glide.stopped")
              .withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    if (!canGlideInCurrentState(player)) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.phantome.glide.airborne")
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    GLIDE_ACTIVE.put(player.getUUID(), true);
    player.startFallFlying();
    player.displayClientMessage(
        Component.translatable("blessing.seroka.phantome.glide.started")
            .withStyle(ChatFormatting.LIGHT_PURPLE),
        true
    );
  }

  public static float applyFlightSpeedDamage(ServerPlayer player, float damage) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PHANTOME)) {
      return damage;
    }
    if (!player.isFallFlying()) {
      return damage;
    }

    double speed = player.getDeltaMovement().horizontalDistance();
    double multiplier = 1.0D + Math.min(MAX_SPEED_DAMAGE_BONUS, speed * SPEED_DAMAGE_FACTOR);
    return (float) (damage * multiplier);
  }

  public static boolean isProtectedFromPhantoms(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.PHANTOME);
  }

  public static void clearPhantomAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PHANTOME)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(PHANTOM_AGGRO_CLEAR_RADIUS);
    for (Phantom phantom : player.serverLevel().getEntitiesOfClass(Phantom.class, area)) {
      if (phantom.getTarget() == player) {
        phantom.setTarget(null);
      }
      if (phantom.getLastHurtByMob() == player) {
        phantom.setLastHurtByMob(null);
      }
    }
  }

  public static void clear(Player player) {
    GLIDE_ACTIVE.remove(player.getUUID());
    if (player.isFallFlying()) {
      player.stopFallFlying();
    }
  }

  private static void tickGlideState(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PHANTOME)) {
      return;
    }
    if (!Boolean.TRUE.equals(GLIDE_ACTIVE.get(player.getUUID()))) {
      return;
    }
    if (player.onGround() || player.isInWater() || player.isPassenger()) {
      stopGliding(player);
    }
  }

  private static void stopGliding(Player player) {
    GLIDE_ACTIVE.put(player.getUUID(), false);
    if (player.isFallFlying()) {
      player.stopFallFlying();
    }
  }

  private static boolean canGlideInCurrentState(Player player) {
    if (player.onGround() || player.isInWater() || player.isPassenger()) {
      return false;
    }
    if (player.hasEffect(MobEffects.LEVITATION)) {
      return false;
    }
    return !player.getAbilities().flying;
  }
}
