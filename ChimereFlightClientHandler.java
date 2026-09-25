package com.seroka.client;

import com.seroka.ModMain;
import com.seroka.chimere.ChimereFlightHelper;
import com.seroka.faction.ModAttachments;
import com.seroka.network.payload.ChimereFlightVanillaPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mayfly doit être actif côté client AVANT le tick joueur (double espace vanilla).
 * Envoie aussi l'état de vol au serveur pour éviter la coupure instantanée.
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class ChimereFlightClientHandler {

  private static final Map<UUID, Boolean> CLIENT_WAS_FLYING = new ConcurrentHashMap<>();

  private ChimereFlightClientHandler() {}

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
    if (!event.getEntity().level().isClientSide()) {
      return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    if (event.getEntity() != minecraft.player) {
      return;
    }
    if (!minecraft.player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }
    ChimereFlightHelper.refreshClientMayfly(minecraft.player);
  }

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public static void onClientTickPre(ClientTickEvent.Pre event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null) {
      return;
    }
    ChimereFlightHelper.ensureVanillaCreativeFlight(minecraft.player);
    if (!minecraft.player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }
    ChimereFlightHelper.refreshClientMayfly(minecraft.player);
  }

  @SubscribeEvent
  public static void onClientTickPost(ClientTickEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null) {
      return;
    }
    if (!minecraft.player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }
    Player player = minecraft.player;
    ChimereFlightHelper.refreshClientMayfly(player);
    syncVanillaFlightToServer(player);
    ChimereFlightHelper.tickFlight(player);
    ChimereFlightHelper.trackFlightTransition(player);
    ChimereFlightHelper.tickFallProtection(player);
  }

  private static void syncVanillaFlightToServer(Player player) {
    if (!ChimereFlightHelper.hasFlightBlessing(player)) {
      CLIENT_WAS_FLYING.remove(player.getUUID());
      return;
    }
    boolean flying = player.getAbilities().flying;
    boolean wasFlying = CLIENT_WAS_FLYING.getOrDefault(player.getUUID(), false);
    // Ne pas propager un flying=false transitoire (ex. paquet serveur reçu ce tick).
    if (!flying && wasFlying && ChimereFlightHelper.canStartFlight(player)) {
      boolean shouldStillFly = ChimereFlightHelper.isFlightModeActive(player)
          || player.getData(ModAttachments.CHIMERE_WAS_FLYING_LAST_TICK);
      if (shouldStillFly) {
        return;
      }
    }
    if (flying != wasFlying) {
      PacketDistributor.sendToServer(new ChimereFlightVanillaPayload(flying));
    }
    CLIENT_WAS_FLYING.put(player.getUUID(), flying);
  }
}
