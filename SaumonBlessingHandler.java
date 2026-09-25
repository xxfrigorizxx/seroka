package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForgeMod;

/** Bénédiction du Dieu Saumon : nage rapide, respiration et vision sous l'eau. */
public final class SaumonBlessingHandler {

  private static final ResourceLocation SWIM_SPEED_ID = ModMain.id("saumon_swim_speed");
  private static final double SWIM_SPEED_BONUS = 0.50D;

  private SaumonBlessingHandler() {}

  public static void applyPassive(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SAUMON)) {
      return;
    }
    setSwimSpeedBonus(player);
    if (player instanceof ServerPlayer serverPlayer) {
      setWaterBreathing(serverPlayer, true);
      if (serverPlayer.isUnderWater()) {
        setUnderwaterNightVision(serverPlayer, true);
      }
    }
  }

  public static void refreshIfNeeded(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SAUMON)) {
      return;
    }
    setSwimSpeedBonus(player);
    if (!(player instanceof ServerPlayer serverPlayer)) {
      return;
    }
    if (!serverPlayer.hasEffect(MobEffects.WATER_BREATHING)) {
      setWaterBreathing(serverPlayer, true);
    }
    if (serverPlayer.isUnderWater()) {
      serverPlayer.setAirSupply(serverPlayer.getMaxAirSupply());
      if (!serverPlayer.hasEffect(MobEffects.NIGHT_VISION)) {
        setUnderwaterNightVision(serverPlayer, true);
      }
      return;
    }
    serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
  }

  public static void clear(Player player) {
    removeSwimSpeedBonus(player);
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.WATER_BREATHING);
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
    }
  }

  private static void setSwimSpeedBonus(Player player) {
    AttributeInstance instance = player.getAttribute(NeoForgeMod.SWIM_SPEED);
    if (instance == null) {
      return;
    }
    if (instance.getModifier(SWIM_SPEED_ID) != null) {
      return;
    }
    instance.addPermanentModifier(
        new AttributeModifier(SWIM_SPEED_ID, SWIM_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );
  }

  private static void removeSwimSpeedBonus(Player player) {
    AttributeInstance instance = player.getAttribute(NeoForgeMod.SWIM_SPEED);
    if (instance != null) {
      instance.removeModifier(SWIM_SPEED_ID);
    }
  }

  private static void setUnderwaterNightVision(ServerPlayer player, boolean enabled) {
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

  private static void setWaterBreathing(ServerPlayer player, boolean enabled) {
    if (!enabled) {
      player.removeEffect(MobEffects.WATER_BREATHING);
      return;
    }
    player.addEffect(new MobEffectInstance(
        MobEffects.WATER_BREATHING,
        MobEffectInstance.INFINITE_DURATION,
        0,
        false,
        false,
        true
    ));
  }
}
