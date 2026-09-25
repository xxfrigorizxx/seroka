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

/** Bénédiction du Dieu Tétard : respiration aquatique, vision nocturne et nage améliorée. */
public final class TetardBlessingHandler {

  private static final ResourceLocation SWIM_SPEED_ID = ModMain.id("tetard_swim_speed");
  private static final double SWIM_SPEED_BONUS = 0.10D;

  private TetardBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.TETARD)) {
      return;
    }
    setSwimSpeedBonus(player);
    setWaterBreathing(player, true);
    setNightVision(player, true);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.TETARD)) {
      return;
    }
    setSwimSpeedBonus(player);
    if (!player.hasEffect(MobEffects.WATER_BREATHING)) {
      setWaterBreathing(player, true);
    }
    if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
      setNightVision(player, true);
    }
    if (player.isUnderWater()) {
      player.setAirSupply(player.getMaxAirSupply());
    }
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
    if (instance == null || instance.getModifier(SWIM_SPEED_ID) != null) {
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
