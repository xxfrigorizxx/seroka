package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Chèvre : chutes sûres, saut et charge frontale. */
public final class ChevreBlessingHandler {

  private static final float SAFE_FALL_BLOCKS = 5.0F;
  private static final double JUMP_VELOCITY_Y = 0.70D;
  private static final double CHARGE_DISTANCE = 5.0D;
  private static final int CHARGE_TICKS = 10;
  private static final double CHARGE_SPEED = CHARGE_DISTANCE / CHARGE_TICKS * 1.15D;
  private static final double KNOCKBACK_STRENGTH = 1.5D;
  private static final int CHARGE_COOLDOWN_TICKS = (2 * 60 + 30) * 20;
  private static final Map<UUID, Long> CHARGE_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, ActiveCharge> ACTIVE_CHARGES = new ConcurrentHashMap<>();

  private ChevreBlessingHandler() {}

  private record ActiveCharge(Vec3 direction, int ticksRemaining) {}

  public static boolean shouldNegateFallDamage(ServerPlayer player, float fallDistance) {
    return ChimereBlessingService.hasBlessing(player, GodIds.CHEVRE) && fallDistance <= SAFE_FALL_BLOCKS;
  }

  public static void activateJump(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CHEVRE)) {
      return;
    }
    if (!player.onGround() && !player.isInWater()) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.chevre.jump.grounded").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    Vec3 motion = player.getDeltaMovement();
    player.setDeltaMovement(motion.x, JUMP_VELOCITY_Y, motion.z);
    player.fallDistance = 0.0F;
    player.hurtMarked = true;
    player.playNotifySound(SoundEvents.GOAT_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.2F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.chevre.jump.used").withStyle(ChatFormatting.GREEN),
        true
    );
  }

  public static void activateCharge(ServerPlayer player, Vec3 direction) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CHEVRE)) {
      return;
    }
    if (ACTIVE_CHARGES.containsKey(player.getUUID())) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - CHARGE_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < CHARGE_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((CHARGE_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.chevre.charge.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
    if (horizontal.lengthSqr() < 1.0E-4D) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.chevre.charge.no_direction").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    Vec3 forward = horizontal.normalize();
    CHARGE_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    ACTIVE_CHARGES.put(player.getUUID(), new ActiveCharge(forward, CHARGE_TICKS));
    applyChargeMotion(player, forward);

    LivingEntity immediateHit = findChargeTarget(player, forward);
    if (immediateHit != null) {
      deliverChargeHit(player, immediateHit, forward);
      ACTIVE_CHARGES.remove(player.getUUID());
      return;
    }

    player.playNotifySound(SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.PLAYERS, 0.9F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.chevre.charge.started").withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static void tickCharge(ServerPlayer player) {
    ActiveCharge charge = ACTIVE_CHARGES.get(player.getUUID());
    if (charge == null) {
      return;
    }

    Vec3 direction = charge.direction();
    LivingEntity hit = findChargeTarget(player, direction);
    if (hit != null) {
      deliverChargeHit(player, hit, direction);
      ACTIVE_CHARGES.remove(player.getUUID());
      return;
    }

    int ticksRemaining = charge.ticksRemaining() - 1;
    if (ticksRemaining <= 0) {
      ACTIVE_CHARGES.remove(player.getUUID());
      return;
    }

    if (player.horizontalCollision) {
      ACTIVE_CHARGES.remove(player.getUUID());
      return;
    }

    applyChargeMotion(player, direction);
    ACTIVE_CHARGES.put(player.getUUID(), new ActiveCharge(direction, ticksRemaining));
  }

  private static void applyChargeMotion(ServerPlayer player, Vec3 direction) {
    Vec3 motion = player.getDeltaMovement();
    player.setDeltaMovement(direction.x * CHARGE_SPEED, motion.y, direction.z * CHARGE_SPEED);
    player.fallDistance = 0.0F;
    player.hurtMarked = true;
  }

  public static void cancelCharge(ServerPlayer player) {
    ACTIVE_CHARGES.remove(player.getUUID());
  }

  public static void clear(Player player) {
    CHARGE_COOLDOWN_LAST_TICK.remove(player.getUUID());
    ACTIVE_CHARGES.remove(player.getUUID());
  }

  private static LivingEntity findChargeTarget(ServerPlayer player, Vec3 chargeDirection) {
    Vec3 forward = chargeDirection.normalize();
    AABB sweep = player.getBoundingBox().inflate(0.35D).expandTowards(forward.scale(2.5D));
    Vec3 origin = player.getEyePosition(1.0F);

    LivingEntity best = null;
    double bestDistance = Double.MAX_VALUE;

    for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, sweep)) {
      if (entity == player || !entity.isAlive() || entity.isSpectator() || !entity.isAttackable()) {
        continue;
      }

      Vec3 toTarget = entity.getBoundingBox().getCenter().subtract(origin);
      Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
      double distance = horizontal.lengthSqr();
      if (distance < 1.0E-4D || distance > 9.0D) {
        continue;
      }

      double alignment = horizontal.normalize().dot(forward);
      if (alignment < 0.35D) {
        continue;
      }

      if (distance < bestDistance) {
        bestDistance = distance;
        best = entity;
      }
    }
    return best;
  }

  private static void deliverChargeHit(ServerPlayer player, LivingEntity target, Vec3 chargeDirection) {
    float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
    if (damage > 0.0F) {
      target.hurt(player.damageSources().playerAttack(player), damage);
    }
    Vec3 knockback = chargeDirection.normalize().scale(KNOCKBACK_STRENGTH);
    target.push(knockback.x, 0.35D, knockback.z);
    target.hurtMarked = true;
    player.swing(InteractionHand.MAIN_HAND, true);
    player.playNotifySound(SoundEvents.GOAT_SCREAMING_HURT, SoundSource.PLAYERS, 1.0F, 0.9F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.chevre.charge.hit", target.getDisplayName())
            .withStyle(ChatFormatting.GRAY),
        true
    );
  }
}
