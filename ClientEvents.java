package com.seroka.client;

import com.seroka.client.screen.FactionSelectionScreen;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Ouvre l'écran de sélection de faction à la première connexion et après chaque mort.
 */
@EventBusSubscriber(modid = com.seroka.ModMain.MODID, value = Dist.CLIENT)
public final class ClientEvents {

  private static boolean pendingFactionScreen;
  private static boolean afterDeathRespawn;

  private ClientEvents() {}

  public static void requestFactionScreenAfterDeath() {
    pendingFactionScreen = true;
    afterDeathRespawn = true;
  }

  @SubscribeEvent
  public static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
    pendingFactionScreen = true;
    afterDeathRespawn = false;
  }

  @SubscribeEvent
  public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
    LocalPlayer oldPlayer = event.getOldPlayer();
    if (oldPlayer != null && oldPlayer.isDeadOrDying()) {
      requestFactionScreenAfterDeath();
    }
  }

  @SubscribeEvent
  public static void onPlayerLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    pendingFactionScreen = false;
    afterDeathRespawn = false;
  }

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    if (!pendingFactionScreen) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null || minecraft.level == null || player.isDeadOrDying()) {
      return;
    }

    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);

    if (!afterDeathRespawn && !faction.isNone()) {
      pendingFactionScreen = false;
      return;
    }

    if (minecraft.screen != null) {
      return;
    }

    minecraft.setScreen(new FactionSelectionScreen());
    pendingFactionScreen = false;
    afterDeathRespawn = false;
  }
}
