package com.seroka.chimere;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Bénédiction du Dieu Slime : anti-chute et dégâts proportionnels aux PV. */
public final class SlimeBlessingHandler {

  private static final double SLIME_AGGRO_CLEAR_RADIUS = 32.0D;

  private SlimeBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SLIME)) {
      return;
    }
    clearSlimeAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SLIME)) {
      return;
    }
    clearSlimeAggro(player);
  }

  public static boolean isProtectedFromSlimes(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.SLIME);
  }

  public static boolean isSlimeDamage(DamageSource source) {
    return source.getEntity() instanceof Slime;
  }

  public static void clearSlimeAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SLIME)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(SLIME_AGGRO_CLEAR_RADIUS);
    for (Slime slime : player.serverLevel().getEntitiesOfClass(Slime.class, area)) {
      if (slime.getTarget() == player) {
        slime.setTarget(null);
      }
      if (slime.getLastHurtByMob() == player) {
        slime.setLastHurtByMob(null);
      }
    }
  }

  public static float applyHealthScaledDamageBonus(ServerPlayer attacker, float damage) {
    if (!ChimereBlessingService.hasBlessing(attacker, GodIds.SLIME)) {
      return damage;
    }
    if (damage <= 0.0F || attacker.getMaxHealth() <= 0.0F) {
      return damage;
    }
    float healthRatio = attacker.getHealth() / attacker.getMaxHealth();
    return damage * (1.0F + healthRatio);
  }

  public static boolean isFallDamage(net.minecraft.world.damagesource.DamageSource source) {
    return source.is(DamageTypes.FALL);
  }
}
