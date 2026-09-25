package com.seroka.client;

import com.seroka.ModMain;
import com.seroka.faction.FactionStaminaHelper;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import com.seroka.network.payload.SideStepPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Double-tap Q/D (gauche/droite) pour le pas de côté Frontalier.
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierSideStepHandler {

  private static final long DOUBLE_TAP_WINDOW_MS = 400L;

  private static long lastLeftPressMs;
  private static long lastRightPressMs;
  private static int localCooldownTicks;

  private FrontalierSideStepHandler() {}

  @SubscribeEvent
  public static void onKeyInput(InputEvent.Key event) {
    if (event.getAction() != GLFW.GLFW_PRESS) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null || minecraft.screen != null) {
      return;
    }

    int key = event.getKey();
    if (key == minecraft.options.keyLeft.getKey().getValue()) {
      handleDirectionPress(player, true);
    } else if (key == minecraft.options.keyRight.getKey().getValue()) {
      handleDirectionPress(player, false);
    }
  }

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    if (localCooldownTicks > 0) {
      localCooldownTicks--;
    }
  }

  private static void handleDirectionPress(LocalPlayer player, boolean left) {
    long now = System.currentTimeMillis();
    if (left) {
      if (now - lastLeftPressMs < DOUBLE_TAP_WINDOW_MS) {
        trySideStep(player, true);
        lastLeftPressMs = 0L;
      } else {
        lastLeftPressMs = now;
      }
      return;
    }

    if (now - lastRightPressMs < DOUBLE_TAP_WINDOW_MS) {
      trySideStep(player, false);
      lastRightPressMs = 0L;
    } else {
      lastRightPressMs = now;
    }
  }

  private static void trySideStep(LocalPlayer player, boolean left) {
    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    if (!SideStepPayload.canSideStep(faction)) {
      return;
    }
    if (!FactionStaminaHelper.hasFood(player, PlayerFaction.SIDE_STEP_MIN_FOOD)) {
      return;
    }
    if (localCooldownTicks > 0) {
      return;
    }

    localCooldownTicks = PlayerFaction.SIDE_STEP_COOLDOWN_TICKS;
    PacketDistributor.sendToServer(new SideStepPayload(left));
    SideStepPayload.applySideStepVelocity(player, left);
    FrontalierSideStepAnimation.triggerLocalSideStep(player, left);
  }
}
