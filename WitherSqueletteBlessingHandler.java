package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Bénédiction du Dieu Wither Squelette : endurance, coups renforcés et wither. */
public final class WitherSqueletteBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("wither_squelette_health");
  private static final double HEALTH_BONUS = 1.0D;
  private static final float MELEE_DAMAGE_MULTIPLIER = 1.5F;
  private static final int WITHER_DURATION_TICKS = 6 * 20;

  private WitherSqueletteBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.WITHER_SQUELETTE)) {
      return;
    }
    applyHealthBonus(player, true);
    extinguishIfBurning(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.WITHER_SQUELETTE)) {
      return;
    }
    applyHealthBonus(player, false);
    extinguishIfBurning(player);
  }

  public static float applyMeleeDamageBonus(ServerPlayer attacker, float damage) {
    if (!ChimereBlessingService.hasBlessing(attacker, GodIds.WITHER_SQUELETTE)) {
      return damage;
    }
    return damage * MELEE_DAMAGE_MULTIPLIER;
  }

  public static void applyWitherOnHit(ServerPlayer attacker, LivingEntity target) {
    if (!ChimereBlessingService.hasBlessing(attacker, GodIds.WITHER_SQUELETTE)) {
      return;
    }
    target.addEffect(new MobEffectInstance(
        MobEffects.WITHER,
        WITHER_DURATION_TICKS,
        0,
        false,
        true,
        true
    ));
  }

  public static boolean isFireLavaOrWitherDamage(DamageSource source) {
    return source.is(DamageTypeTags.IS_FIRE)
        || source.is(DamageTypes.LAVA)
        || source.is(DamageTypes.WITHER);
  }

  public static void clear(Player player) {
    AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
    if (health != null) {
      health.removeModifier(HEALTH_ID);
    }
    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
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

  private static void extinguishIfBurning(ServerPlayer player) {
    if (player.isOnFire()) {
      player.clearFire();
    }
  }
}
