package com.seroka.client;

import com.seroka.faction.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;

/**
 * UI flottante Frontalier partagée par inventaire, tables de craft, fours, etc.
 */
public final class FrontalierContainerUi {

  private static int appliedOffsetX;
  private static int appliedOffsetY;

  private FrontalierContainerUi() {}

  public static boolean isEnabled() {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null) {
      return false;
    }
    return player.getData(ModAttachments.PLAYER_FACTION).isFrontalier();
  }

  public static void applyScreenOffset(AbstractContainerScreen<?> screen, float offsetX, float offsetY) {
    appliedOffsetX = Math.round(offsetX);
    appliedOffsetY = Math.round(offsetY);
    screen.leftPos += appliedOffsetX;
    screen.topPos += appliedOffsetY;
  }

  public static void clearScreenOffset(AbstractContainerScreen<?> screen) {
    if (appliedOffsetX != 0 || appliedOffsetY != 0) {
      screen.leftPos -= appliedOffsetX;
      screen.topPos -= appliedOffsetY;
      appliedOffsetX = 0;
      appliedOffsetY = 0;
    }
  }
}
