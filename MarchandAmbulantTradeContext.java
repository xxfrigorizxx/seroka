package com.seroka.chimere;

import net.minecraft.world.entity.player.Player;

/** Contexte thread-local pendant un échange marchand (villageois) pour les mixins. */
public final class MarchandAmbulantTradeContext {

  private static final ThreadLocal<Player> ACTIVE = new ThreadLocal<>();

  private MarchandAmbulantTradeContext() {}

  public static void open(Player player) {
    if (MarchandAmbulantBlessingHandler.isActive(player)) {
      ACTIVE.set(player);
    }
  }

  public static void close() {
    ACTIVE.remove();
  }

  public static boolean isActive() {
    Player player = ACTIVE.get();
    return player != null && MarchandAmbulantBlessingHandler.isActive(player);
  }
}
