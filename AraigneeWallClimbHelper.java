package com.seroka.chimere;

import com.seroka.faction.RollMath;
import com.seroka.faction.WallJumpMath;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Grimpe murale des bénédictions Araignée / Araignée empoisonnée. */
public final class AraigneeWallClimbHelper {

  private static final double CLIMB_SPEED = 0.20D;

  private AraigneeWallClimbHelper() {}

  public static boolean hasWallClimbBlessing(Player player) {
    return ChimereBlessingService.hasBlessing(player, GodIds.ARAIGNEE)
        || ChimereBlessingService.hasBlessing(player, GodIds.ARAIGNEE_EMPOISONNEE);
  }

  public static void refresh(Player player) {
    if (!hasWallClimbBlessing(player) || player.isInWater() || player.isInLava()) {
      return;
    }
    if (!isTouchingWall(player)) {
      return;
    }

    Vec3 motion = player.getDeltaMovement();
    boolean movingIntoWall = RollMath.hasMovementInput(player);

    if (movingIntoWall && !player.isShiftKeyDown()) {
      player.setDeltaMovement(motion.x, CLIMB_SPEED, motion.z);
    } else if (player.isShiftKeyDown()) {
      player.setDeltaMovement(motion.x, -CLIMB_SPEED, motion.z);
    } else if (!player.onGround()) {
      player.setDeltaMovement(motion.x, 0.0D, motion.z);
    }
    player.fallDistance = 0.0F;
  }

  private static boolean isTouchingWall(Player player) {
    if (player.horizontalCollision) {
      return true;
    }
    return WallJumpMath.findWallEscapeDirection(player) != null;
  }
}
