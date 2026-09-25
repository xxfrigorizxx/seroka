package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Bénédiction du Dieu Zoglin : résistance, force et neutralité avec les hoglins. */
public final class ZoglinBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("zoglin_health");
  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("zoglin_attack_damage");
  private static final double HEALTH_BONUS = 2.0D;
  private static final double ATTACK_DAMAGE_BONUS = 0.2D;
  private static final double HOGLIN_AGGRO_CLEAR_RADIUS = 32.0D;

  private ZoglinBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ZOGLIN)) {
      return;
    }
    applyHealthBonus(player, true);
    applyAttackDamageBonus(player);
    extinguishIfBurning(player);
    clearHoglinAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ZOGLIN)) {
      return;
    }
    applyHealthBonus(player, false);
    applyAttackDamageBonus(player);
    extinguishIfBurning(player);
    clearHoglinAggro(player);
  }

  public static boolean isProtectedFromHoglins(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.ZOGLIN);
  }

  public static boolean isFireOrLavaDamage(DamageSource source) {
    return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA);
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
  }

  public static void clearHoglinAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ZOGLIN)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(HOGLIN_AGGRO_CLEAR_RADIUS);
    for (Hoglin hoglin : player.serverLevel().getEntitiesOfClass(Hoglin.class, area)) {
      if (hoglin.getTarget() == player) {
        hoglin.setTarget(null);
      }
      if (hoglin.getLastHurtByMob() == player) {
        hoglin.setLastHurtByMob(null);
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

  private static void extinguishIfBurning(ServerPlayer player) {
    if (player.isOnFire()) {
      player.clearFire();
    }
  }
}
