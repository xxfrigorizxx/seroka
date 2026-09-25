package com.seroka.chimere;

import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Bénédiction du Dieu Grenouille : respiration x2, vision sous l'eau et saut puissant. */
public final class GrenouilleBlessingHandler {

  private static final int JUMP_COOLDOWN_TICKS = 5 * 20;
  private static final int BREATHING_MULTIPLIER = 2;
  private static final double JUMP_VELOCITY_Y = 0.58D;

  private GrenouilleBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GRENOUILLE)) {
      return;
    }
    refreshAquaticPassives(player);
  }

  public static void activateJump(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GRENOUILLE)) {
      return;
    }
    if (!player.onGround() && !player.isInWater()) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.grenouille.jump.grounded").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    long now = player.level().getGameTime();
    long elapsed = now - state.grenouilleJumpLastTick();
    if (elapsed < JUMP_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((JUMP_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.grenouille.jump.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    player.setData(ModAttachments.CHIMERE_BLESSING_STATE, state.withGrenouilleJumpLastTick(now));
    applyJumpVelocity(player);
    player.playNotifySound(SoundEvents.FROG_LONG_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GRENOUILLE)) {
      return;
    }
    refreshAquaticPassives(player);
    compensateUnderwaterBreathing(player);
  }

  public static void clear(Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
    }
  }

  public static void applyJumpVelocity(Player player) {
    Vec3 motion = player.getDeltaMovement();
    player.setDeltaMovement(motion.x, JUMP_VELOCITY_Y, motion.z);
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.hurtMarked = true;
    }
  }

  private static void refreshAquaticPassives(ServerPlayer player) {
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
    if (player.level().getGameTime() % BREATHING_MULTIPLIER != 0) {
      return;
    }
    int maxAir = player.getMaxAirSupply();
    if (player.getAirSupply() < maxAir) {
      player.setAirSupply(Math.min(player.getAirSupply() + 1, maxAir));
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
