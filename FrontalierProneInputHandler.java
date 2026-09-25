package com.seroka.client;

import com.seroka.faction.FactionProneHandler;
import com.seroka.faction.ModAttachments;
import com.seroka.network.payload.TogglePronePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Double-tap Sneak (Shift) pour basculer le ramper Frontalier.
 */
@EventBusSubscriber(modid = com.seroka.ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierProneInputHandler {

  private static final long DOUBLE_TAP_WINDOW_MS = 300L;

  private static long lastSneakPressMs;

  private FrontalierProneInputHandler() {}

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    Player player = minecraft.player;
    if (player == null) {
      return;
    }
    if (!player.getData(ModAttachments.PLAYER_FACTION).isFrontalier()) {
      return;
    }

    var faction = player.getData(ModAttachments.PLAYER_FACTION);
    if (faction.isRolling()) {
      FactionProneHandler.forceStanding(player);
      return;
    }

    if (faction.isProne() && !player.isInWater() && player.getDeltaMovement().y > 0.0) {
      FactionProneHandler.exitProne(player, faction);
      return;
    }

    if (faction.isProne()) {
      if (!player.isInWater()) {
        FactionProneHandler.applyPronePose(player);
      }
    } else if (player.getForcedPose() == net.minecraft.world.entity.Pose.SWIMMING) {
      FactionProneHandler.clearPronePose(player);
    }
  }

  @SubscribeEvent
  public static void onKeyInput(InputEvent.Key event) {
    if (event.getAction() != InputConstants.PRESS) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.screen != null) {
      return;
    }
    if (!minecraft.player.getData(ModAttachments.PLAYER_FACTION).isFrontalier()) {
      return;
    }

    InputConstants.Key pressedKey = InputConstants.getKey(event.getKey(), event.getScanCode());
    if (!minecraft.options.keyShift.isActiveAndMatches(pressedKey)) {
      return;
    }

    long now = System.currentTimeMillis();
    if (now - lastSneakPressMs < DOUBLE_TAP_WINDOW_MS) {
      PacketDistributor.sendToServer(new TogglePronePayload());
      lastSneakPressMs = 0L;
    } else {
      lastSneakPressMs = now;
    }
  }
}
