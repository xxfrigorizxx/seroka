package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Tatou : résistance et invulnérabilité brève. */
public final class TatouBlessingHandler {

  private static final float INCOMING_DAMAGE_MULTIPLIER = 0.9F;
  private static final int SHELL_DURATION_TICKS = 1 * 20;
  private static final int SHELL_COOLDOWN_TICKS = 2 * 60 * 20;
  private static final Map<UUID, Long> SHELL_UNTIL_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> SHELL_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private TatouBlessingHandler() {}

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.TATOU)) {
      return;
    }
    long now = player.level().getGameTime();
    Long until = SHELL_UNTIL_TICK.get(player.getUUID());
    if (until != null && now >= until) {
      SHELL_UNTIL_TICK.remove(player.getUUID());
    }
  }

  public static float reduceIncomingDamage(ServerPlayer player, float damage) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.TATOU)) {
      return damage;
    }
    if (isShellActive(player)) {
      return 0.0F;
    }
    return damage * INCOMING_DAMAGE_MULTIPLIER;
  }

  public static void activateShell(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.TATOU)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - SHELL_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < SHELL_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((SHELL_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.tatou.shell.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    SHELL_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    SHELL_UNTIL_TICK.put(player.getUUID(), now + SHELL_DURATION_TICKS);
    player.playNotifySound(SoundEvents.ARMADILLO_ROLL, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.tatou.shell.used").withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static boolean isShellActive(ServerPlayer player) {
    Long until = SHELL_UNTIL_TICK.get(player.getUUID());
    return until != null && player.level().getGameTime() < until;
  }

  public static void clear(Player player) {
    SHELL_UNTIL_TICK.remove(player.getUUID());
    SHELL_COOLDOWN_LAST_TICK.remove(player.getUUID());
  }
}
