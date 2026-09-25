package com.seroka.chimere;

import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bénédiction du Dieu Calamar : aquatique, impulsion (F) et jet d'encre (clic molette). */
public final class CalamarBlessingHandler {

  private static final int BOOST_COOLDOWN_TICKS = 20;
  private static final int INK_COOLDOWN_TICKS = 5 * 20;
  private static final double BOOST_STRENGTH = 1.75D;
  private static final double INK_RADIUS = 5.0D;
  private static final int INK_BLINDNESS_TICKS = 80;
  private static final int INK_PARTICLE_COUNT = 48;

  private CalamarBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CALAMAR)) {
      return;
    }
    setWaterBreathing(player, true);
    if (player.isUnderWater()) {
      setUnderwaterNightVision(player, true);
    }
  }

  public static void activateBoost(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CALAMAR)) {
      return;
    }
    if (!player.isUnderWater()) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.calamar.boost.underwater_only").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    long now = player.level().getGameTime();
    long elapsed = now - state.calamarBoostLastTick();
    if (elapsed < BOOST_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((BOOST_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.calamar.boost.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    player.setData(ModAttachments.CHIMERE_BLESSING_STATE, state.withCalamarBoostLastTick(now));
    applyBoostVelocity(player);
    player.playNotifySound(SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 0.5F, 1.2F);
  }

  public static void activateInk(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CALAMAR)) {
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    long now = player.level().getGameTime();
    long elapsed = now - state.calamarInkLastTick();
    if (elapsed < INK_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((INK_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.calamar.ink.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    player.setData(ModAttachments.CHIMERE_BLESSING_STATE, state.withCalamarInkLastTick(now));
    releaseInk(player);
    player.playNotifySound(SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 1.0F, 0.9F);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CALAMAR)) {
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
    }
  }

  public static void applyBoostVelocity(Player player) {
    Vec3 direction = player.getLookAngle().normalize();
    Vec3 motion = player.getDeltaMovement();
    player.setDeltaMovement(
        motion.x + direction.x * BOOST_STRENGTH,
        motion.y + direction.y * BOOST_STRENGTH,
        motion.z + direction.z * BOOST_STRENGTH
    );
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.hurtMarked = true;
    }
  }

  private static void releaseInk(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    Vec3 look = player.getLookAngle().normalize();
    Vec3 origin = player.getEyePosition().add(look.scale(0.75D));
    Vec3 cloudCenter = origin.add(look.scale(2.0D));

    for (int i = 0; i < INK_PARTICLE_COUNT; i++) {
      double spreadX = (level.random.nextDouble() - 0.5D) * 1.6D;
      double spreadY = (level.random.nextDouble() - 0.5D) * 1.6D;
      double spreadZ = (level.random.nextDouble() - 0.5D) * 1.6D;
      double velocityX = (level.random.nextDouble() - 0.5D) * 0.08D;
      double velocityY = (level.random.nextDouble() - 0.5D) * 0.08D;
      double velocityZ = (level.random.nextDouble() - 0.5D) * 0.08D;
      level.sendParticles(
          ParticleTypes.SQUID_INK,
          cloudCenter.x + spreadX,
          cloudCenter.y + spreadY,
          cloudCenter.z + spreadZ,
          1,
          velocityX,
          velocityY,
          velocityZ,
          0.0D
      );
    }

    AABB area = new AABB(cloudCenter, cloudCenter).inflate(INK_RADIUS);
    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
      if (target == player) {
        continue;
      }
      if (target.distanceToSqr(cloudCenter) > INK_RADIUS * INK_RADIUS) {
        continue;
      }
      target.addEffect(new MobEffectInstance(
          MobEffects.BLINDNESS,
          INK_BLINDNESS_TICKS,
          0,
          false,
          true,
          true
      ));
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
