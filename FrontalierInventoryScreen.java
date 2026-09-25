package com.seroka.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;

/**
 * Inventaire joueur Frontaliers : pas de filtre sombre, aperçu personnage unique.
 */
public class FrontalierInventoryScreen extends InventoryScreen {

  private static final int PREVIEW_BG_COLOR = 0xFFC6C6C6;
  private static final int PREVIEW_X1_OFFSET = 26;
  private static final int PREVIEW_Y1_OFFSET = 4;
  private static final int PREVIEW_X2_OFFSET = 75;
  private static final int PREVIEW_Y2_OFFSET = 78;
  private static final int PLAYER_PREVIEW_SCALE = 28;
  private static final float PLAYER_PREVIEW_Y_OFFSET = -0.12f;

  public FrontalierInventoryScreen(Player player) {
    super(player);
  }

  @Override
  public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
  }

  @Override
  protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    int left = this.leftPos;
    int top = this.topPos;
    guiGraphics.blit(INVENTORY_LOCATION, left, top, 0, 0, this.imageWidth, this.imageHeight);

    int previewX1 = left + PREVIEW_X1_OFFSET;
    int previewY1 = top + PREVIEW_Y1_OFFSET;
    int previewX2 = left + PREVIEW_X2_OFFSET;
    int previewY2 = top + PREVIEW_Y2_OFFSET;

    guiGraphics.fill(previewX1, previewY1, previewX2, previewY2, PREVIEW_BG_COLOR);

    if (this.minecraft.player != null) {
      InventoryScreen.renderEntityInInventoryFollowsMouse(
          guiGraphics,
          previewX1,
          previewY1,
          previewX2,
          previewY2,
          PLAYER_PREVIEW_SCALE,
          PLAYER_PREVIEW_Y_OFFSET,
          mouseX,
          mouseY,
          this.minecraft.player
      );
    }
  }
}
