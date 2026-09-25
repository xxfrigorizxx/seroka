package com.seroka.chimere;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Bénédiction du Dieu Squelette : vision nocturne, arc infini et flèches dévastatrices. */
public final class SqueletteBlessingHandler {

  private static final double SKELETON_AGGRO_CLEAR_RADIUS = 32.0D;

  private SqueletteBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SQUELETTE)) {
      return;
    }
    refreshNightVision(player);
    ArcInfiniHelper.ensureIncarnationKit(player);
    clearSkeletonAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SQUELETTE)) {
      return;
    }
    refreshNightVision(player);
    ArcInfiniHelper.ensureSingleArrow(player);
    if (player.tickCount % 40 == 0) {
      ArcInfiniHelper.ensureIncarnationKit(player);
    }
  }

  public static boolean shouldDoubleBowDamage(DamageSource source, ServerPlayer attacker) {
    if (!ChimereBlessingService.hasBlessing(attacker, GodIds.SQUELETTE)) {
      return false;
    }
    if (!(source.getDirectEntity() instanceof AbstractArrow)) {
      return false;
    }
    if (source.getEntity() != attacker) {
      return false;
    }
    ItemStack weapon = source.getWeaponItem();
    return !weapon.isEmpty() && weapon.getItem() instanceof BowItem;
  }

  public static float applyBowDamageBonus(float damage) {
    return damage * 2.0F;
  }

  public static void onArrowShot(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SQUELETTE)) {
      return;
    }
    ArcInfiniHelper.ensureSingleArrow(player);
  }

  public static boolean isProtectedFromSkeletons(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.SQUELETTE);
  }

  public static boolean isSkeletonAggressor(DamageSource source) {
    if (source.getEntity() instanceof AbstractSkeleton) {
      return true;
    }
    if (source.getDirectEntity() instanceof AbstractArrow arrow) {
      return arrow.getOwner() instanceof AbstractSkeleton;
    }
    return false;
  }

  public static void clearSkeletonAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SQUELETTE)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(SKELETON_AGGRO_CLEAR_RADIUS);
    for (AbstractSkeleton skeleton : player.serverLevel().getEntitiesOfClass(AbstractSkeleton.class, area)) {
      if (skeleton.getTarget() == player) {
        skeleton.setTarget(null);
      }
      if (skeleton.getLastHurtByMob() == player) {
        skeleton.setLastHurtByMob(null);
      }
    }
  }

  public static void clear(Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
    }
    ArcInfiniHelper.clearKit(player);
  }

  private static void refreshNightVision(ServerPlayer player) {
    if (isNight(player)) {
      if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
        setNightVision(player, true);
      }
      return;
    }
    player.removeEffect(MobEffects.NIGHT_VISION);
  }

  private static boolean isNight(ServerPlayer player) {
    return player.level().isNight();
  }

  private static void setNightVision(ServerPlayer player, boolean enabled) {
    if (!enabled) {
      player.removeEffect(MobEffects.NIGHT_VISION);
      return;
    }
    player.addEffect(new MobEffectInstance(
        MobEffects.NIGHT_VISION,
        MobEffectInstance.INFINITE_DURATION,
        0,
        false,
        false,
        true
    ));
  }
}
