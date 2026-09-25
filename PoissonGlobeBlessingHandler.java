package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForgeMod;

/** Bénédiction du Dieu Poisson-globe : aquatique + aura de poison au clic molette. */
public final class PoissonGlobeBlessingHandler {

  private static final ResourceLocation SWIM_SPEED_ID = ModMain.id("poisson_globe_swim_speed");
  private static final double SWIM_SPEED_BONUS = 0.10D;
  private static final int POISON_COOLDOWN_TICKS = 2 * 60 * 20;
  private static final int POISON_DURATION_TICKS = 30 * 20;
  private static final double POISON_RADIUS = 2.0D;
  private static final int POISON_EFFECT_DURATION = 10 * 20;
  private static final int POISON_AMPLIFIER = 1;

  private PoissonGlobeBlessingHandler() {}

  public static void applyPassive(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.POISSON_GLOBE)) {
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

  public static void activate(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.POISSON_GLOBE)) {
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    long now = player.level().getGameTime();
    long elapsed = now - state.poissonGlobePoisonLastTick();
    if (elapsed < POISON_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((POISON_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.poisson_globe.poison.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    long activeUntil = now + POISON_DURATION_TICKS;
    player.setData(
        ModAttachments.CHIMERE_BLESSING_STATE,
        state.withPoissonGlobePoisonLastTick(now).withPoissonGlobePoisonActiveUntil(activeUntil)
    );
    player.playNotifySound(SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.PLAYERS, 0.8F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.poisson_globe.poison.active").withStyle(ChatFormatting.DARK_GREEN),
        true
    );
  }

  public static void refreshIfNeeded(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.POISSON_GLOBE)) {
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
    } else {
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
    }
    if (isPoisonAuraActive(serverPlayer)) {
      tickPoisonAura(serverPlayer);
    }
  }

  public static void clear(Player player) {
    removeSwimSpeedBonus(player);
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.WATER_BREATHING);
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
    }
  }

  private static boolean isPoisonAuraActive(ServerPlayer player) {
    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    return player.level().getGameTime() < state.poissonGlobePoisonActiveUntil();
  }

  private static void tickPoisonAura(ServerPlayer player) {
    AABB area = player.getBoundingBox().inflate(POISON_RADIUS);
    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
      if (target == player) {
        continue;
      }
      if (player.distanceTo(target) > POISON_RADIUS) {
        continue;
      }
      MobEffectInstance existing = target.getEffect(MobEffects.POISON);
      int duration = POISON_EFFECT_DURATION;
      int amplifier = POISON_AMPLIFIER;
      if (existing != null) {
        duration = Math.max(duration, existing.getDuration());
        amplifier = Math.max(amplifier, existing.getAmplifier());
      }
      target.addEffect(new MobEffectInstance(
          MobEffects.POISON,
          duration,
          amplifier,
          false,
          true,
          true
      ));
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
