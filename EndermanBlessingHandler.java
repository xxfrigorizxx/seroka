package com.seroka.chimere;

import com.seroka.ModMain;
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
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Enderman : endurance, force et téléportation. */
public final class EndermanBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("enderman_health");
  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("enderman_attack_damage");
  private static final double HEALTH_BONUS = 0.5D;
  private static final double ATTACK_DAMAGE_BONUS = 0.25D;
  private static final double TELEPORT_MAX_DISTANCE = 20.0D;
  private static final int TELEPORT_COOLDOWN_TICKS = 60 * 20;
  private static final double ENDERMAN_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final Map<UUID, Long> TELEPORT_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private EndermanBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDERMAN)) {
      return;
    }
    applyHealthBonus(player, true);
    applyAttackDamageBonus(player);
    clearEndermanAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDERMAN)) {
      return;
    }
    applyHealthBonus(player, false);
    applyAttackDamageBonus(player);
    clearEndermanAggro(player);
  }

  public static boolean isProtectedFromEndermen(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.ENDERMAN);
  }

  public static boolean isEndermanDamage(DamageSource source) {
    return source.getEntity() instanceof EnderMan || source.getDirectEntity() instanceof EnderMan;
  }

  public static void clearEndermanAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDERMAN)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(ENDERMAN_AGGRO_CLEAR_RADIUS);
    for (EnderMan enderman : player.serverLevel().getEntitiesOfClass(EnderMan.class, area)) {
      if (enderman.getTarget() == player) {
        enderman.setTarget(null);
      }
      if (enderman.getLastHurtByMob() == player) {
        enderman.setLastHurtByMob(null);
      }
      if (player.getUUID().equals(enderman.getPersistentAngerTarget())) {
        enderman.setPersistentAngerTarget(null);
        enderman.setRemainingPersistentAngerTime(0);
      }
      enderman.stopBeingAngry();
    }
  }

  public static void activateTeleport(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDERMAN)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - TELEPORT_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < TELEPORT_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((TELEPORT_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.enderman.teleport.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    Vec3 destination = resolveTeleportDestination(player);
    if (destination == null) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.enderman.teleport.blocked").withStyle(ChatFormatting.RED),
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
    player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.enderman.teleport.used").withStyle(ChatFormatting.DARK_PURPLE),
        true
    );
  }

  public static void clear(Player player) {
    AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
    if (health != null) {
      health.removeModifier(HEALTH_ID);
    }
    AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attack != null) {
      attack.removeModifier(ATTACK_DAMAGE_ID);
    }
    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
    }
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
        24,
        0.5D,
        0.5D,
        0.5D,
        0.1D
    );
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

  private static void applyAttackDamageBonus(Player player) {
    AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (instance == null) {
      return;
    }
    instance.removeModifier(ATTACK_DAMAGE_ID);
    instance.addPermanentModifier(
        new AttributeModifier(ATTACK_DAMAGE_ID, ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );
  }
}
