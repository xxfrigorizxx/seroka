package com.seroka.chimere;

import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Bénédiction du Dieu Calamar luminescent : aquatique, impulsion (G) et lueur (clic molette). */
public final class CalamarLuminescentBlessingHandler {

  private static final int BOOST_COOLDOWN_TICKS = 20;
  private static final int GLOWING_DURATION_TICKS = 60 * 20;

  private CalamarLuminescentBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CALAMAR_LUMINESCENT)) {
      return;
    }
    setWaterBreathing(player, true);
    if (player.isUnderWater()) {
      setUnderwaterNightVision(player, true);
    }
  }

  public static void activateBoost(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CALAMAR_LUMINESCENT)) {
      return;
    }
    if (!player.isUnderWater()) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.calamar_luminescent.boost.underwater_only")
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    long now = player.level().getGameTime();
    long elapsed = now - state.calamarLuminescentBoostLastTick();
    if (elapsed < BOOST_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((BOOST_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.calamar_luminescent.boost.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    player.setData(ModAttachments.CHIMERE_BLESSING_STATE, state.withCalamarLuminescentBoostLastTick(now));
    CalamarBlessingHandler.applyBoostVelocity(player);
    player.playNotifySound(SoundEvents.GLOW_SQUID_AMBIENT, SoundSource.PLAYERS, 0.7F, 1.1F);
  }

  public static void activateGlowing(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CALAMAR_LUMINESCENT)) {
      return;
    }

    player.addEffect(new MobEffectInstance(
        MobEffects.GLOWING,
        GLOWING_DURATION_TICKS,
        0,
        false,
        true,
        true
    ));
    player.playNotifySound(SoundEvents.GLOW_SQUID_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.9F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.calamar_luminescent.glow.active").withStyle(ChatFormatting.AQUA),
        true
    );
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CALAMAR_LUMINESCENT)) {
      return;
    }
    if (!player.hasEffect(MobEffects.WATER_BREATHING)) {
      setWaterBreathing(player, true);
    }
    if (player.isUnderWater()) {
      player.setAirSupply(player.getMaxAirSupply());
      if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
        setUnderwaterNightVision(player, true);
      }
      return;
    }
    player.removeEffect(MobEffects.NIGHT_VISION);
  }

  public static void clear(Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.WATER_BREATHING);
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
      serverPlayer.removeEffect(MobEffects.GLOWING);
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
