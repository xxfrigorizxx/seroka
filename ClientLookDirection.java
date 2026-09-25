package com.seroka.client;

import com.seroka.faction.RollMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Direction horizontale visée par le joueur pour les capacités Chimère (crosshair / WASD). */
public final class ClientLookDirection {

  private ClientLookDirection() {}

  public static Vec3 getHorizontal(Minecraft minecraft) {
    LocalPlayer player = minecraft.player;
    if (player == null) {
      return Vec3.ZERO;
    }

    if (RollMath.hasMovementInput(player)) {
      return RollMath.getMovementDirection(player);
    }

    Vec3 horizontal = Vec3.directionFromRotation(0.0F, player.getYRot());
    if (minecraft.options.getCameraType().isMirrored()) {
      horizontal = horizontal.scale(-1.0D);
    }
    return horizontal.lengthSqr() > 1.0E-6D ? horizontal.normalize() : Vec3.ZERO;
  }
}
