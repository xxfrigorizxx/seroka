package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForgeMod;

/** Bénédiction du Dieu Dauphin : nage rapide, aura de grâce et impulsion au clic molette. */
public final class DauphinBlessingHandler {

  private static final ResourceLocation SWIM_SPEED_ID = ModMain.id("dauphin_swim_speed");
  private static final double SWIM_SPEED_BONUS = 1.0D;
  private static final double GRACE_RADIUS = 50.0D;
  private static final int GRACE_REFRESH_TICKS = 40;
  private static final int BOOST_COOLDOWN_TICKS = 20;
  private static final int BREATHING_MULTIPLIER = 10;

  private DauphinBlessingHandler() {}

  public static void applyPassive(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.DAUPHIN)) {
      return;
    }
    setSwimSpeedBonus(player);
    if (player instanceof ServerPlayer serverPlayer) {
      refreshUnderwaterVision(serverPlayer);
    }
  }

  public static void activateImpulse(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.DAUPHIN)) {
      return;
    }
    if (!player.isUnderWater()) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.dauphin.boost.underwater_only").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    long now = player.level().getGameTime();
    long elapsed = now - state.dauphinBoostLastTick();
    if (elapsed < BOOST_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((BOOST_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.dauphin.boost.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    player.setData(ModAttachments.CHIMERE_BLESSING_STATE, state.withDauphinBoostLastTick(now));
    CalamarBlessingHandler.applyBoostVelocity(player);
    player.playNotifySound(SoundEvents.DOLPHIN_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.2F);
  }

  public static void refreshIfNeeded(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.DAUPHIN)) {
      return;
    }
    setSwimSpeedBonus(player);
    if (!(player instanceof ServerPlayer serverPlayer)) {
      return;
    }
    refreshUnderwaterVision(serverPlayer);
    compensateUnderwaterBreathing(serverPlayer);
    tickGraceAura(serverPlayer);
  }

  public static void clear(Player player) {
    removeSwimSpeedBonus(player);
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
      removeAuraGrace(serverPlayer);
    }
  }

  private static void refreshUnderwaterVision(ServerPlayer player) {
    if (player.isUnderWater()) {
      if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
        setUnderwaterNightVision(player, true);
      }
      return;
    }
    player.removeEffect(MobEffects.NIGHT_VISION);
  }

  private static void compensateUnderwaterBreathing(ServerPlayer player) {
    if (!player.isEyeInFluid(FluidTags.WATER)) {
      return;
    }
    if (player.level().getGameTime() % BREATHING_MULTIPLIER == 0) {
      return;
    }
    int maxAir = player.getMaxAirSupply();
    if (player.getAirSupply() < maxAir) {
      player.setAirSupply(Math.min(player.getAirSupply() + 1, maxAir));
    }
  }

  private static void tickGraceAura(ServerPlayer dolphin) {
    ServerLevel level = dolphin.serverLevel();
    AABB area = dolphin.getBoundingBox().inflate(GRACE_RADIUS);
    for (ServerPlayer target : level.getEntitiesOfClass(ServerPlayer.class, area)) {
      if (!target.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
        continue;
      }
      if (!target.getData(ModAttachments.CHIMERE_BLESSING).isActive()) {
        continue;
      }
      if (dolphin.distanceTo(target) > GRACE_RADIUS) {
        continue;
      }
      target.addEffect(new MobEffectInstance(
          MobEffects.DOLPHINS_GRACE,
          GRACE_REFRESH_TICKS,
          0,
          false,
          false,
          true
      ));
    }
  }

  private static void removeAuraGrace(ServerPlayer player) {
    MobEffectInstance effect = player.getEffect(MobEffects.DOLPHINS_GRACE);
    if (effect != null && !effect.isAmbient()) {
      player.removeEffect(MobEffects.DOLPHINS_GRACE);
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
}
