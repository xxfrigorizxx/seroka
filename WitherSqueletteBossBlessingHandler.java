package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Wither Squelette Boss : vol, endurance et tête de Wither. */
public final class WitherSqueletteBossBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("wither_squelette_boss_health");
  private static final ResourceLocation FLYING_SPEED_ID = ModMain.id("wither_squelette_boss_flying_speed");
  private static final double HEALTH_BONUS = 4.0D;
  private static final double FLYING_SPEED_MULTIPLIER = -0.05D;
  private static final float INCOMING_DAMAGE_MULTIPLIER = 0.4F;
  private static final double AGGRO_CLEAR_RADIUS = 48.0D;
  private static final int SKULL_COOLDOWN_TICKS = 30 * 20;

  private static final Map<UUID, Long> SKULL_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private WitherSqueletteBossBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.WITHER_SQUELETTE_BOSS)) {
      return;
    }
    applyHealthBonus(player, true);
    applyFlyingSpeedModifier(player);
    enableFlight(player);
    clearSkeletonAggro(player);
    clearWitherAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.WITHER_SQUELETTE_BOSS)) {
      return;
    }
    applyHealthBonus(player, false);
    applyFlyingSpeedModifier(player);
    enableFlight(player);
    clearSkeletonAggro(player);
    clearWitherAggro(player);
  }

  public static void activateWitherSkull(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.WITHER_SQUELETTE_BOSS)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - SKULL_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < SKULL_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((SKULL_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.wither_squelette_boss.skull.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    launchWitherSkull(player);
    SKULL_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.wither_squelette_boss.skull.used")
            .withStyle(ChatFormatting.DARK_GRAY),
        true
    );
  }

  public static float reduceIncomingDamage(ServerPlayer player, float damage) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.WITHER_SQUELETTE_BOSS)) {
      return damage;
    }
    return damage * INCOMING_DAMAGE_MULTIPLIER;
  }

  public static boolean isProtectedFromSkeletons(LivingEntity entity) {
    return entity instanceof Player player
        && ChimereBlessingService.hasBlessing(player, GodIds.WITHER_SQUELETTE_BOSS);
  }

  public static boolean isSkeletonAggressor(DamageSource source) {
    if (source.getEntity() instanceof AbstractSkeleton) {
      return true;
    }
    if (source.getDirectEntity() instanceof AbstractSkeleton) {
      return true;
    }
    if (source.getDirectEntity() instanceof AbstractArrow arrow) {
      return arrow.getOwner() instanceof AbstractSkeleton;
    }
    if (source.getDirectEntity() instanceof WitherSkull skull) {
      var owner = skull.getOwner();
      return owner instanceof AbstractSkeleton || owner instanceof WitherBoss;
    }
    return false;
  }

  public static boolean isFallOrWitherDamage(DamageSource source) {
    return source.is(DamageTypes.FALL) || source.is(DamageTypes.WITHER);
  }

  public static void clear(ServerPlayer player) {
    SKULL_COOLDOWN_LAST_TICK.remove(player.getUUID());
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

  private static void launchWitherSkull(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    Vec3 look = player.getViewVector(1.0F);
    WitherSkull skull = new WitherSkull(level, player, look);
    skull.setDangerous(true);
    skull.moveTo(player.getX(), player.getEyeY() - 0.1D, player.getZ());
    level.addFreshEntity(skull);
  }

  private static void clearSkeletonAggro(ServerPlayer player) {
    AABB area = player.getBoundingBox().inflate(AGGRO_CLEAR_RADIUS);
    for (AbstractSkeleton skeleton : player.serverLevel().getEntitiesOfClass(AbstractSkeleton.class, area)) {
      if (skeleton.getTarget() == player) {
        skeleton.setTarget(null);
      }
      if (skeleton.getLastHurtByMob() == player) {
        skeleton.setLastHurtByMob(null);
      }
    }
  }

  private static void clearWitherAggro(ServerPlayer player) {
    AABB area = player.getBoundingBox().inflate(AGGRO_CLEAR_RADIUS);
    for (WitherBoss wither : player.serverLevel().getEntitiesOfClass(WitherBoss.class, area)) {
      if (wither.getTarget() == player) {
        wither.setTarget(null);
      }
      if (wither.getLastHurtByMob() == player) {
        wither.setLastHurtByMob(null);
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
}
