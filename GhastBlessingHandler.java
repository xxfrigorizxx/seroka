package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Ghast : vol lent et boule de feu. */
public final class GhastBlessingHandler {

  private static final ResourceLocation FLYING_SPEED_ID = ModMain.id("ghast_flying_speed");
  private static final double FLYING_SPEED_MULTIPLIER = -0.5D;
  private static final double GHAST_AGGRO_CLEAR_RADIUS = 64.0D;
  private static final int FIREBALL_COOLDOWN_TICKS = 25 * 20;
  private static final Map<UUID, Long> FIREBALL_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private GhastBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GHAST)) {
      return;
    }
    applyFlyingSpeedModifier(player);
    enableFlight(player);
    clearGhastAggro(player);
    extinguishIfBurning(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GHAST)) {
      return;
    }
    applyFlyingSpeedModifier(player);
    enableFlight(player);
    clearGhastAggro(player);
    extinguishIfBurning(player);
  }

  public static boolean isProtectedFromGhasts(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.GHAST);
  }

  public static boolean isFireOrLavaDamage(DamageSource source) {
    return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA);
  }

  public static void clearGhastAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GHAST)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(GHAST_AGGRO_CLEAR_RADIUS);
    for (Ghast ghast : player.serverLevel().getEntitiesOfClass(Ghast.class, area)) {
      if (ghast.getTarget() == player) {
        ghast.setTarget(null);
      }
      if (ghast.getLastHurtByMob() == player) {
        ghast.setLastHurtByMob(null);
      }
    }
  }

  private static void extinguishIfBurning(ServerPlayer player) {
    if (player.isOnFire()) {
      player.clearFire();
    }
  }

  public static void activateFireball(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GHAST)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - FIREBALL_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < FIREBALL_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((FIREBALL_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.ghast.fireball.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    launchFireball(player);
    FIREBALL_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.ghast.fireball.used")
            .withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static void clear(Player player) {
    FIREBALL_COOLDOWN_LAST_TICK.remove(player.getUUID());
    removeFlyingSpeedModifier(player);
    if (player instanceof ServerPlayer serverPlayer) {
      disableFlight(serverPlayer);
    }
  }

  private static void launchFireball(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    Vec3 look = player.getViewVector(1.0F);
    LargeFireball fireball = new LargeFireball(level, player, look, 1);
    fireball.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
    level.addFreshEntity(fireball);
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
