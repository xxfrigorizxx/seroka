package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForgeMod;

/** Bénédiction du Dieu Tortue : aquatique modéré, carapace défensive au clic molette. */
public final class TortueBlessingHandler {

  private static final ResourceLocation SWIM_SPEED_ID = ModMain.id("tortue_swim_speed");
  private static final ResourceLocation SHELL_MOVE_SLOW_ID = ModMain.id("tortue_shell_move_slow");
  private static final ResourceLocation SHELL_SWIM_SLOW_ID = ModMain.id("tortue_shell_swim_slow");
  private static final double SWIM_SPEED_BONUS = 0.20D;
  private static final double SHELL_SLOW_PENALTY = -0.50D;
  private static final float SHELL_DAMAGE_MULTIPLIER = 0.25F;
  private static final int SHELL_DURATION_TICKS = 5 * 60 * 20;
  private static final int SHELL_COOLDOWN_TICKS = 5 * 60 * 20;

  private TortueBlessingHandler() {}

  public static void applyPassive(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.TORTUE)) {
      return;
    }
    if (player instanceof ServerPlayer serverPlayer) {
      refreshAquaticPassives(serverPlayer);
    }
  }

  public static void activate(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.TORTUE)) {
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    long now = player.level().getGameTime();
    if (isShellActive(player)) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.tortue.shell.active").withStyle(ChatFormatting.GREEN),
          true
      );
      return;
    }

    long elapsed = now - state.tortueShellLastTick();
    if (elapsed < SHELL_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((SHELL_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.tortue.shell.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    long activeUntil = now + SHELL_DURATION_TICKS;
    player.setData(
        ModAttachments.CHIMERE_BLESSING_STATE,
        state.withTortueShellLastTick(now).withTortueShellActiveUntil(activeUntil)
    );
    applyShellSlow(player);
    player.playNotifySound(SoundEvents.TURTLE_SWIM, SoundSource.PLAYERS, 1.0F, 0.9F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.tortue.shell.activated").withStyle(ChatFormatting.DARK_AQUA),
        true
    );
  }

  public static void refreshIfNeeded(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.TORTUE)) {
      return;
    }
    if (player instanceof ServerPlayer serverPlayer) {
      refreshAquaticPassives(serverPlayer);
      refreshShellEffects(serverPlayer);
      compensateUnderwaterBreathing(serverPlayer);
    }
  }

  public static void clear(Player player) {
    removeSwimSpeedBonus(player);
    removeShellSlow(player);
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
    }
  }

  public static boolean isShellActive(ServerPlayer player) {
    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    return player.level().getGameTime() < state.tortueShellActiveUntil();
  }

  public static float applyShellDamageReduction(ServerPlayer player, float damage) {
    if (!isShellActive(player)) {
      return damage;
    }
    return damage * SHELL_DAMAGE_MULTIPLIER;
  }

  private static void refreshAquaticPassives(ServerPlayer player) {
    if (player.isUnderWater()) {
      setSwimSpeedBonus(player);
      if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
        setUnderwaterNightVision(player, true);
      }
      return;
    }
    removeSwimSpeedBonus(player);
    player.removeEffect(MobEffects.NIGHT_VISION);
  }

  private static void refreshShellEffects(ServerPlayer player) {
    if (isShellActive(player)) {
      applyShellSlow(player);
      return;
    }
    removeShellSlow(player);
  }

  private static void compensateUnderwaterBreathing(ServerPlayer player) {
    if (!player.isEyeInFluid(FluidTags.WATER)) {
      return;
    }
    if (player.level().getGameTime() % 3 == 0) {
      return;
    }
    int maxAir = player.getMaxAirSupply();
    if (player.getAirSupply() < maxAir) {
      player.setAirSupply(Math.min(player.getAirSupply() + 1, maxAir));
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

  private static void applyShellSlow(Player player) {
    AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (moveSpeed != null && moveSpeed.getModifier(SHELL_MOVE_SLOW_ID) == null) {
      moveSpeed.addPermanentModifier(
          new AttributeModifier(SHELL_MOVE_SLOW_ID, SHELL_SLOW_PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
      );
    }
    AttributeInstance swimSpeed = player.getAttribute(NeoForgeMod.SWIM_SPEED);
    if (swimSpeed != null && swimSpeed.getModifier(SHELL_SWIM_SLOW_ID) == null) {
      swimSpeed.addPermanentModifier(
          new AttributeModifier(SHELL_SWIM_SLOW_ID, SHELL_SLOW_PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
      );
    }
  }

  private static void removeShellSlow(Player player) {
    AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (moveSpeed != null) {
      moveSpeed.removeModifier(SHELL_MOVE_SLOW_ID);
    }
    AttributeInstance swimSpeed = player.getAttribute(NeoForgeMod.SWIM_SPEED);
    if (swimSpeed != null) {
      swimSpeed.removeModifier(SHELL_SWIM_SLOW_ID);
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
