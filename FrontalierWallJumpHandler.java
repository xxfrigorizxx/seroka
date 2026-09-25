package com.seroka.client;

import com.seroka.faction.FactionStaminaHelper;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import com.seroka.faction.WallJumpMath;
import com.seroka.network.payload.WallJumpPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Rebond mural Frontalier — uniquement en l'air, loin du sol.
 */
@EventBusSubscriber(modid = com.seroka.ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierWallJumpHandler {

  private static int localCooldown;
  private static int localAirTicks;
  private static boolean wasOnGround;

  private FrontalierWallJumpHandler() {}

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    if (localCooldown > 0) {
      localCooldown--;
    }

    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null || minecraft.screen != null) {
      return;
    }

    updateAirTicks(player);

    while (minecraft.options.keyJump.consumeClick()) {
      if (wasOnGround) {
        continue;
      }
      tryWallJump(player);
    }

    wasOnGround = player.onGround() || !WallJumpMath.hasGroundClearance(player);
  }

  private static void updateAirTicks(LocalPlayer player) {
    if (player.onGround() || !WallJumpMath.hasGroundClearance(player)) {
      localAirTicks = 0;
      return;
    }
    localAirTicks++;
  }

  private static void tryWallJump(LocalPlayer player) {
    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    if (!canAttemptWallJump(player, faction)) {
      return;
    }

    var impulse = WallJumpMath.computeWallPush(player);
    if (impulse == null) {
      return;
    }

    localCooldown = WallJumpPayload.COOLDOWN_TICKS;
    WallJumpPayload.applyWallJumpVelocity(player, impulse);
    FrontalierWallJumpAnimation.triggerLocalWallJump(player);
    PacketDistributor.sendToServer(new WallJumpPayload());
  }

  private static boolean canAttemptWallJump(LocalPlayer player, PlayerFaction faction) {
    return WallJumpPayload.canWallJump(faction)
        && localCooldown <= 0
        && localAirTicks >= WallJumpMath.MIN_AIR_TICKS
        && WallJumpMath.isTrulyAirborne(player)
        && player.horizontalCollision
        && !player.isInWater()
        && !faction.isProne()
        && !faction.isRolling()
        && FactionStaminaHelper.hasFood(player, WallJumpMath.MIN_FOOD)
        && WallJumpMath.computeWallPush(player) != null;
  }
}
