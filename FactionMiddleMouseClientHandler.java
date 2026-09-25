package com.seroka.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.seroka.ModMain;
import com.seroka.faction.DagueInfinieHelper;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import com.seroka.client.ClientLookDirection;
import com.seroka.network.payload.ChimereGodAbilityPayload;
import com.seroka.network.payload.ThrowSwordPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Clic molette (clic 3) des factions — intercepté avant le pick-block vanilla qui bloque
 * souvent {@link KeyMapping#consumeClick()} en Survie.
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class FactionMiddleMouseClientHandler {

  private static int lastHandledTick = -1;

  private FactionMiddleMouseClientHandler() {}

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public static void onMiddleMousePress(InputEvent.MouseButton.Pre event) {
    if (event.getAction() != InputConstants.PRESS) {
      return;
    }
    if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null || minecraft.screen != null) {
      return;
    }

    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    if (faction.isTetrasomie()) {
      return;
    }

    if (player.tickCount == lastHandledTick) {
      event.setCanceled(true);
      return;
    }

    if (faction.isChimere()) {
      sendChimereAbility(minecraft, player);
      lastHandledTick = player.tickCount;
      event.setCanceled(true);
      return;
    }

    if (faction.isFrontalier() && DagueInfinieHelper.canThrowHeldWeapon(player, faction)) {
      PacketDistributor.sendToServer(new ThrowSwordPayload());
      FrontalierThrowSwordAnimation.triggerLocalThrow(player);
      lastHandledTick = player.tickCount;
      event.setCanceled(true);
    }
  }

  private static void sendChimereAbility(Minecraft minecraft, LocalPlayer player) {
    Vec3 look = ClientLookDirection.getHorizontal(minecraft);
    PacketDistributor.sendToServer(new ChimereGodAbilityPayload(
        player.isShiftKeyDown(),
        (float) look.x,
        (float) look.z
    ));
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
    if (KeyInputHandler.throwSwordKey == null || event.getKeyMapping() != KeyInputHandler.throwSwordKey) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null || minecraft.screen != null) {
      return;
    }

    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    if (faction.isTetrasomie() || player.tickCount == lastHandledTick) {
      return;
    }

    if (faction.isChimere()) {
      sendChimereAbility(minecraft, player);
      lastHandledTick = player.tickCount;
      event.setCanceled(true);
      return;
    }

    if (faction.isFrontalier() && DagueInfinieHelper.canThrowHeldWeapon(player, faction)) {
      PacketDistributor.sendToServer(new ThrowSwordPayload());
      FrontalierThrowSwordAnimation.triggerLocalThrow(player);
      lastHandledTick = player.tickCount;
      event.setCanceled(true);
    }
  }
}
