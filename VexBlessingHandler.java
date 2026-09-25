package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Vex : vol et intangibilité. */
public final class VexBlessingHandler {

  private static final ResourceLocation FLYING_SPEED_ID = ModMain.id("vex_flying_speed");
  private static final double FLYING_SPEED_MULTIPLIER = -0.4D;
  private static final double VEX_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final int INTANGIBLE_DURATION_TICKS = 10 * 20;
  private static final int INTANGIBLE_COOLDOWN_TICKS = 10 * 60 * 20;
  private static final Map<UUID, Long> INTANGIBLE_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private VexBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VEX)) {
      return;
    }
    applyFlyingSpeedModifier(player);
    enableFlight(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VEX)) {
      return;
    }
    applyFlyingSpeedModifier(player);
    enableFlight(player);
    tickIntangibility(player);
    if (isIntangible(player)) {
      clearVexAggro(player);
    }
  }

  public static void refreshIntangibilityPhysics(Player player) {
    if (!GodIds.VEX.equals(player.getData(ModAttachments.CHIMERE_BLESSING).godId())) {
      return;
    }
    applyIntangibilityPhysics(player);
  }

  public static boolean isIntangible(Player player) {
    long until = player.getData(ModAttachments.VEX_INTANGIBLE_UNTIL);
    return until > 0L && player.level().getGameTime() < until;
  }

  public static boolean isProtectedFromVexes(LivingEntity entity) {
    return entity instanceof Player player
        && ChimereBlessingService.hasBlessing(player, GodIds.VEX)
        && isIntangible(player);
  }

  public static boolean isVexDamage(DamageSource source) {
    return source.getEntity() instanceof Vex || source.getDirectEntity() instanceof Vex;
  }

  public static void activateIntangibility(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VEX)) {
      return;
    }

    long now = player.level().getGameTime();
    if (isIntangible(player)) {
      return;
    }

    long elapsed = now - INTANGIBLE_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < INTANGIBLE_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((INTANGIBLE_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.vex.intangible.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    INTANGIBLE_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    setIntangibleUntil(player, now + INTANGIBLE_DURATION_TICKS);
    applyIntangibilityPhysics(player);
    spawnIntangibilityParticles(player.serverLevel(), player);
    player.playNotifySound(SoundEvents.VEX_CHARGE, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.vex.intangible.used").withStyle(ChatFormatting.AQUA),
        true
    );
  }

  public static void clear(Player player) {
    INTANGIBLE_COOLDOWN_LAST_TICK.remove(player.getUUID());
    setIntangibleUntil(player, 0L);
    removeFlyingSpeedModifier(player);
    if (player instanceof ServerPlayer serverPlayer) {
      endIntangibility(serverPlayer);
      disableFlight(serverPlayer);
    } else {
      player.noPhysics = false;
    }
  }

  private static void tickIntangibility(ServerPlayer player) {
    applyIntangibilityPhysics(player);

    if (!isIntangible(player)) {
      return;
    }

    long now = player.level().getGameTime();
    long until = player.getData(ModAttachments.VEX_INTANGIBLE_UNTIL);
    if (now >= until) {
      setIntangibleUntil(player, 0L);
      endIntangibility(player);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.vex.intangible.ended").withStyle(ChatFormatting.GRAY),
          true
      );
    }
  }

  private static void applyIntangibilityPhysics(Player player) {
    if (isIntangible(player)) {
      player.noPhysics = true;
      player.fallDistance = 0.0F;
      player.setOnGround(false);
      player.horizontalCollision = false;
      player.verticalCollision = false;
      player.verticalCollisionBelow = false;
      if (player instanceof ServerPlayer serverPlayer) {
        serverPlayer.setInvulnerable(true);
      }
      return;
    }

    if (player instanceof ServerPlayer serverPlayer) {
      if (serverPlayer.noPhysics || serverPlayer.isInvulnerable()) {
        endIntangibility(serverPlayer);
      }
      return;
    }

    long until = player.getData(ModAttachments.VEX_INTANGIBLE_UNTIL);
    if (until == 0L || player.level().getGameTime() >= until) {
      player.noPhysics = false;
    }
  }

  private static void setIntangibleUntil(Player player, long untilTick) {
    player.setData(ModAttachments.VEX_INTANGIBLE_UNTIL, untilTick);
  }

  private static void endIntangibility(ServerPlayer player) {
    player.noPhysics = false;
    player.setInvulnerable(false);
    escapeIfInsideBlocks(player);
  }

  private static void escapeIfInsideBlocks(ServerPlayer player) {
    int attempts = 0;
    while (player.isInWall() && attempts < 16) {
      player.teleportTo(player.getX(), player.getY() + 1.0D, player.getZ());
      attempts++;
    }
  }

  private static void clearVexAggro(ServerPlayer player) {
    AABB area = player.getBoundingBox().inflate(VEX_AGGRO_CLEAR_RADIUS);
    for (Vex vex : player.serverLevel().getEntitiesOfClass(Vex.class, area)) {
      if (vex.getTarget() == player) {
        vex.setTarget(null);
      }
      if (vex.getLastHurtByMob() == player) {
        vex.setLastHurtByMob(null);
      }
    }
  }

  private static void spawnIntangibilityParticles(ServerLevel level, ServerPlayer player) {
    level.sendParticles(
        ParticleTypes.SOUL,
        player.getX(),
        player.getY() + player.getBbHeight() / 2.0D,
        player.getZ(),
        24,
        0.4D,
        0.5D,
        0.4D,
        0.05D
    );
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
