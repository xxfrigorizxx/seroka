package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Ender Dragon : vol, endurance et souffle du dragon. */
public final class EnderDragonBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("ender_dragon_health");
  private static final ResourceLocation FLYING_SPEED_ID = ModMain.id("ender_dragon_flying_speed");
  private static final double HEALTH_BONUS = 5.0D;
  private static final double FLYING_SPEED_MULTIPLIER = 0.10D;
  private static final float INCOMING_DAMAGE_MULTIPLIER = 0.5F;
  private static final float OUTGOING_DAMAGE_MULTIPLIER = 2.5F;
  private static final double AGGRO_CLEAR_RADIUS = 64.0D;
  private static final int FIREBALL_COOLDOWN_TICKS = 30 * 20;
  private static final int BREATH_CLOUD_INTERVAL_TICKS = 4;
  private static final float BREATH_DAMAGE = 2.0F;
  private static final double BREATH_RANGE = 6.0D;

  private static final Map<UUID, Long> FIREBALL_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> BREATH_COOLDOWN_UNTIL_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> BREATH_START_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Boolean> BREATH_ACTIVE = new ConcurrentHashMap<>();

  private EnderDragonBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDER_DRAGON)) {
      return;
    }
    applyHealthBonus(player, true);
    applyFlyingSpeedModifier(player);
    enableFlight(player);
    clearDragonAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDER_DRAGON)) {
      return;
    }
    applyHealthBonus(player, false);
    applyFlyingSpeedModifier(player);
    enableFlight(player);
    clearDragonAggro(player);
    tickBreath(player);
  }

  public static void toggleBreath(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDER_DRAGON)) {
      return;
    }

    long now = player.level().getGameTime();
    if (Boolean.TRUE.equals(BREATH_ACTIVE.get(player.getUUID()))) {
      stopBreath(player, now);
      return;
    }

    long cooldownUntil = BREATH_COOLDOWN_UNTIL_TICK.getOrDefault(player.getUUID(), 0L);
    if (now < cooldownUntil) {
      long remainingTicks = cooldownUntil - now;
      player.displayClientMessage(
          Component.translatable("blessing.seroka.ender_dragon.breath.cooldown", formatCooldown(remainingTicks))
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    BREATH_ACTIVE.put(player.getUUID(), true);
    BREATH_START_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.6F, 1.2F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.ender_dragon.breath.started")
            .withStyle(ChatFormatting.LIGHT_PURPLE),
        true
    );
  }

  public static void activateFireball(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDER_DRAGON)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - FIREBALL_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < FIREBALL_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((FIREBALL_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.ender_dragon.fireball.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    launchFireball(player);
    FIREBALL_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.ender_dragon.fireball.used")
            .withStyle(ChatFormatting.DARK_PURPLE),
        true
    );
  }

  public static float reduceIncomingDamage(ServerPlayer player, float damage) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDER_DRAGON)) {
      return damage;
    }
    return damage * INCOMING_DAMAGE_MULTIPLIER;
  }

  public static float boostOutgoingDamage(ServerPlayer player, float damage) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDER_DRAGON)) {
      return damage;
    }
    return damage * OUTGOING_DAMAGE_MULTIPLIER;
  }

  public static boolean isProtectedFromDragon(LivingEntity entity) {
    return entity instanceof Player player
        && ChimereBlessingService.hasBlessing(player, GodIds.ENDER_DRAGON);
  }

  public static boolean isDragonAggressor(DamageSource source) {
    if (source.is(DamageTypes.DRAGON_BREATH)) {
      return true;
    }
    if (source.getEntity() instanceof EnderDragon) {
      return true;
    }
    if (source.getDirectEntity() instanceof EnderDragon) {
      return true;
    }
    return source.getDirectEntity() instanceof DragonFireball
        && source.getEntity() instanceof EnderDragon;
  }

  public static boolean isImmuneToBreathDamage(ServerPlayer player, DamageSource source) {
    if (isDragonAggressor(source)) {
      return true;
    }
    if (source.getEntity() == player || source.getDirectEntity() == player) {
      return true;
    }
    if (source.getDirectEntity() instanceof AreaEffectCloud cloud && cloud.getOwner() == player) {
      return true;
    }
    return false;
  }

  public static void clear(ServerPlayer player) {
    long now = player.level().getGameTime();
    if (Boolean.TRUE.equals(BREATH_ACTIVE.remove(player.getUUID()))) {
      stopBreath(player, now);
    } else {
      FIREBALL_COOLDOWN_LAST_TICK.remove(player.getUUID());
      BREATH_COOLDOWN_UNTIL_TICK.remove(player.getUUID());
      BREATH_START_TICK.remove(player.getUUID());
      BREATH_ACTIVE.remove(player.getUUID());
    }
    removeFlyingSpeedModifier(player);
    disableFlight(player);
    AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
    if (health != null) {
      health.removeModifier(HEALTH_ID);
    }
    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
    }
  }

  private static void tickBreath(ServerPlayer player) {
    if (!Boolean.TRUE.equals(BREATH_ACTIVE.get(player.getUUID()))) {
      return;
    }

    long now = player.level().getGameTime();
    long start = BREATH_START_TICK.getOrDefault(player.getUUID(), now);
    if ((now - start) % BREATH_CLOUD_INTERVAL_TICKS != 0) {
      return;
    }

    spawnBreathCloud(player);
    applyBreathDamage(player);
    if ((now - start) % 20 == 0) {
      player.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.25F, 1.4F);
    }
  }

  private static void stopBreath(ServerPlayer player, long now) {
    long start = BREATH_START_TICK.getOrDefault(player.getUUID(), now);
    long duration = Math.max(0L, now - start);

    BREATH_ACTIVE.put(player.getUUID(), false);
    BREATH_START_TICK.remove(player.getUUID());
    BREATH_COOLDOWN_UNTIL_TICK.put(player.getUUID(), now + duration);

    player.displayClientMessage(
        Component.translatable("blessing.seroka.ender_dragon.breath.stopped", formatCooldown(duration))
            .withStyle(ChatFormatting.GRAY),
        true
    );
  }

  private static void launchFireball(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    Vec3 look = player.getViewVector(1.0F);
    DragonFireball fireball = new DragonFireball(level, player, look);
    fireball.moveTo(player.getX(), player.getEyeY() - 0.1D, player.getZ());
    level.addFreshEntity(fireball);
  }

  private static void spawnBreathCloud(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    Vec3 look = player.getViewVector(1.0F);
    Vec3 spawn = player.getEyePosition().add(look.scale(2.0D));

    AreaEffectCloud cloud = new AreaEffectCloud(level, spawn.x, spawn.y, spawn.z);
    cloud.setOwner(player);
    cloud.setParticle(ParticleTypes.DRAGON_BREATH);
    cloud.setRadius(3.0F);
    cloud.setDuration(100);
    cloud.setWaitTime(0);
    cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
    level.addFreshEntity(cloud);
  }

  private static void applyBreathDamage(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    Vec3 look = player.getViewVector(1.0F).normalize();
    Vec3 origin = player.getEyePosition();
    AABB area = player.getBoundingBox().expandTowards(look.scale(BREATH_RANGE)).inflate(2.0D, 1.5D, 2.0D);
    DamageSource source = level.damageSources().source(DamageTypes.DRAGON_BREATH, player);

    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
      if (target == player || !target.isAlive() || target.isSpectator() || !target.isAttackable()) {
        continue;
      }

      Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
      Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
      Vec3 horizontalToTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);
      if (horizontalLook.lengthSqr() < 1.0E-6D || horizontalToTarget.lengthSqr() < 1.0E-6D) {
        continue;
      }

      double alignment = horizontalToTarget.normalize().dot(horizontalLook.normalize());
      if (alignment < 0.35D) {
        continue;
      }
      if (origin.distanceToSqr(target.getBoundingBox().getCenter()) > BREATH_RANGE * BREATH_RANGE) {
        continue;
      }

      target.hurt(source, BREATH_DAMAGE);
    }
  }

  private static void clearDragonAggro(ServerPlayer player) {
    AABB area = player.getBoundingBox().inflate(AGGRO_CLEAR_RADIUS);
    for (EnderDragon dragon : player.serverLevel().getEntitiesOfClass(EnderDragon.class, area)) {
      if (dragon.getTarget() == player) {
        dragon.setTarget(null);
      }
      if (dragon.getLastHurtByMob() == player) {
        dragon.setLastHurtByMob(null);
      }
    }
  }

  private static void applyHealthBonus(Player player, boolean fillToMax) {
    AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
    if (instance == null) {
      return;
    }

    float ratio = player.getMaxHealth() > 0.0F ? player.getHealth() / player.getMaxHealth() : 1.0F;
    instance.removeModifier(HEALTH_ID);
    instance.addPermanentModifier(
        new AttributeModifier(HEALTH_ID, HEALTH_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );

    if (fillToMax) {
      player.setHealth(player.getMaxHealth());
    } else {
      player.setHealth(Math.min(player.getMaxHealth(), player.getMaxHealth() * ratio));
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

  private static String formatCooldown(long ticks) {
    if (ticks < 20L) {
      return ticks + "t";
    }
    long seconds = ticks / 20L;
    if (seconds < 60L) {
      return seconds + "s";
    }
    long minutes = seconds / 60L;
    long remainingSeconds = seconds % 60L;
    if (minutes < 60L) {
      return remainingSeconds > 0L ? minutes + "m " + remainingSeconds + "s" : minutes + "m";
    }
    long hours = minutes / 60L;
    long remainingMinutes = minutes % 60L;
    return remainingMinutes > 0L ? hours + "h " + remainingMinutes + "m" : hours + "h";
  }
}
