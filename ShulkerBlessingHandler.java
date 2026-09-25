package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Shulker : résistance et projectile poursuivant. */
public final class ShulkerBlessingHandler {

  private static final float INCOMING_DAMAGE_MULTIPLIER = 0.5F;
  private static final double SHULKER_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final double BULLET_TARGET_RANGE = 32.0D;
  private static final int BULLET_COOLDOWN_TICKS = 35 * 20;
  private static final Map<UUID, Long> BULLET_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private ShulkerBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SHULKER)) {
      return;
    }
    clearShulkerAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SHULKER)) {
      return;
    }
    clearShulkerAggro(player);
  }

  public static float reduceIncomingDamage(ServerPlayer player, float damage) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SHULKER)) {
      return damage;
    }
    return damage * INCOMING_DAMAGE_MULTIPLIER;
  }

  public static boolean isProtectedFromShulkers(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.SHULKER);
  }

  public static boolean isShulkerDamage(DamageSource source) {
    if (source.getEntity() instanceof Shulker) {
      return true;
    }
    if (source.getDirectEntity() instanceof ShulkerBullet bullet) {
      Entity owner = bullet.getOwner();
      return owner instanceof Shulker;
    }
    return false;
  }

  public static void clearShulkerAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SHULKER)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(SHULKER_AGGRO_CLEAR_RADIUS);
    for (Shulker shulker : player.serverLevel().getEntitiesOfClass(Shulker.class, area)) {
      if (shulker.getTarget() == player) {
        shulker.setTarget(null);
      }
      if (shulker.getLastHurtByMob() == player) {
        shulker.setLastHurtByMob(null);
      }
    }
  }

  public static void activateBullet(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SHULKER)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - BULLET_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < BULLET_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((BULLET_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.shulker.bullet.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    LivingEntity target = findBulletTarget(player);
    if (target == null) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.shulker.bullet.no_target").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    launchBullet(player, target);
    BULLET_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.shulker.bullet.used", target.getDisplayName())
            .withStyle(ChatFormatting.LIGHT_PURPLE),
        true
    );
  }

  public static void clear(Player player) {
    BULLET_COOLDOWN_LAST_TICK.remove(player.getUUID());
  }

  private static void launchBullet(ServerPlayer player, Entity target) {
    Vec3 toTarget = target.getEyePosition(1.0F).subtract(player.getEyePosition(1.0F));
    Direction.Axis axis = Direction.getNearest(toTarget.x, toTarget.y, toTarget.z).getAxis();
    ShulkerBullet bullet = new ShulkerBullet(player.serverLevel(), player, target, axis);
    bullet.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
    player.serverLevel().addFreshEntity(bullet);
  }

  private static LivingEntity findBulletTarget(ServerPlayer player) {
    Vec3 eye = player.getEyePosition(1.0F);
    Vec3 look = player.getViewVector(1.0F);
    Vec3 end = eye.add(look.scale(BULLET_TARGET_RANGE));
    AABB searchBox = player.getBoundingBox().expandTowards(look.scale(BULLET_TARGET_RANGE)).inflate(1.0D);
    EntityHitResult hit = ProjectileUtil.getEntityHitResult(
        player,
        eye,
        end,
        searchBox,
        entity -> entity instanceof LivingEntity living
            && living != player
            && living.isAlive()
            && !living.isSpectator()
            && entity.isPickable(),
        BULLET_TARGET_RANGE * BULLET_TARGET_RANGE
    );
    if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
      return null;
    }
    return target;
  }
}
