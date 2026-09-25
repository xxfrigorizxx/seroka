package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Golem de Neige : lancer de boules de neige. */
public final class GolemNeigeBlessingHandler {

  private static final int SNOWBALL_COOLDOWN_TICKS = 2 * 20;
  private static final Map<UUID, Long> SNOWBALL_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private GolemNeigeBlessingHandler() {}

  public static void activateSnowball(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GOLEM_NEIGE)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - SNOWBALL_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < SNOWBALL_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((SNOWBALL_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.golem_neige.snowball.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    launchSnowball(player);
    SNOWBALL_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
  }

  public static void clear(Player player) {
    SNOWBALL_COOLDOWN_LAST_TICK.remove(player.getUUID());
  }

  private static void launchSnowball(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    Snowball snowball = new Snowball(level, player);
    snowball.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
    Vec3 look = player.getViewVector(1.0F);
    snowball.shoot(look.x, look.y, look.z, 1.5F, 1.0F);
    level.addFreshEntity(snowball);
  }
}
