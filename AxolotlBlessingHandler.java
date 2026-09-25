package com.seroka.chimere;

import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bénédiction du Dieu Axolote : aquatique, faire le mort (G) et régénération (clic molette). */
public final class AxolotlBlessingHandler {

  private static final int ABILITY_COOLDOWN_TICKS = 10 * 60 * 20;
  private static final int REGEN_DURATION_TICKS = 60 * 20;
  private static final double MOVEMENT_THRESHOLD_SQR = 0.05D * 0.05D;
  private static final double MOB_FORGET_RADIUS = 64.0D;

  private AxolotlBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.AXOLOTL)) {
      return;
    }
    setWaterBreathing(player, true);
    if (player.isUnderWater()) {
      setUnderwaterNightVision(player, true);
    }
  }

  public static void activatePlayDead(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.AXOLOTL)) {
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    if (state.axolotlPlayDeadActive()) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.axolotl.play_dead.active").withStyle(ChatFormatting.GREEN),
          true
      );
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - state.axolotlPlayDeadCooldownLastTick();
    if (elapsed < ABILITY_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((ABILITY_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.axolotl.play_dead.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    Vec3 position = player.position();
    ChimereBlessingState.PlayDeadRuntimeState playDead = new ChimereBlessingState.PlayDeadRuntimeState(
        true,
        position.x,
        position.y,
        position.z
    );
    player.setData(
        ModAttachments.CHIMERE_BLESSING_STATE,
        state.withAxolotlPlayDeadCooldownLastTick(now).withAxolotlPlayDead(playDead)
    );
    forgetPlayerFromMobs(player);
    player.playNotifySound(SoundEvents.AXOLOTL_DEATH, SoundSource.PLAYERS, 0.8F, 1.1F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.axolotl.play_dead.activated").withStyle(ChatFormatting.DARK_AQUA),
        true
    );
  }

  public static void activateRegen(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.AXOLOTL)) {
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    long now = player.level().getGameTime();
    long elapsed = now - state.axolotlRegenCooldownLastTick();
    if (elapsed < ABILITY_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((ABILITY_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.axolotl.regen.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    player.setData(ModAttachments.CHIMERE_BLESSING_STATE, state.withAxolotlRegenCooldownLastTick(now));
    player.addEffect(new MobEffectInstance(
        MobEffects.REGENERATION,
        REGEN_DURATION_TICKS,
        0,
        false,
        true,
        true
    ));
    player.playNotifySound(SoundEvents.AXOLOTL_IDLE_WATER, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.axolotl.regen.active").withStyle(ChatFormatting.LIGHT_PURPLE),
        true
    );
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.AXOLOTL)) {
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
    } else {
      player.removeEffect(MobEffects.NIGHT_VISION);
    }
    tickPlayDead(player);
  }

  public static void clear(Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.WATER_BREATHING);
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
      serverPlayer.removeEffect(MobEffects.REGENERATION);
    }
  }

  private static void tickPlayDead(ServerPlayer player) {
    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    if (!state.axolotlPlayDeadActive()) {
      return;
    }

    ChimereBlessingState.PlayDeadRuntimeState playDead = state.axolotl().playDead();
    Vec3 anchor = new Vec3(playDead.anchorX(), playDead.anchorY(), playDead.anchorZ());
    if (hasMoved(player, anchor)) {
      player.setData(
          ModAttachments.CHIMERE_BLESSING_STATE,
          state.withAxolotlPlayDead(ChimereBlessingState.PlayDeadRuntimeState.DEFAULT.cleared())
      );
      player.displayClientMessage(
          Component.translatable("blessing.seroka.axolotl.play_dead.broken").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    forgetPlayerFromMobs(player);
  }

  private static boolean hasMoved(ServerPlayer player, Vec3 anchor) {
    if (player.position().distanceToSqr(anchor) > MOVEMENT_THRESHOLD_SQR) {
      return true;
    }
    return player.getDeltaMovement().lengthSqr() > 1.0E-4D;
  }

  private static void forgetPlayerFromMobs(ServerPlayer player) {
    AABB area = player.getBoundingBox().inflate(MOB_FORGET_RADIUS);
    for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area)) {
      if (mob.getTarget() == player) {
        mob.setTarget(null);
      }
      if (mob.getLastHurtByMob() == player) {
        mob.setLastHurtByMob(null);
      }
      if (mob instanceof NeutralMob neutralMob) {
        neutralMob.setRemainingPersistentAngerTime(0);
        neutralMob.setPersistentAngerTarget(null);
      }
      mob.setLastHurtByPlayer(null);
    }
    for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
      if (living.getLastHurtByMob() == player) {
        living.setLastHurtByMob(null);
      }
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
