package com.seroka.client.screen;

import com.seroka.chimere.AnePackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 9x2 sac de bât + inventaire joueur (comme un petit coffre vanilla). */
public class AnePackScreen extends AbstractContainerScreen<AnePackMenu> {

  private static final ResourceLocation TEXTURE =
      ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
  private static final int PLAYER_PANEL_HEIGHT = 96;
  private static final int PLAYER_PANEL_TEXTURE_Y = 126;

  public AnePackScreen(AnePackMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    int containerHeight = AnePackMenu.PACK_ROWS * 18 + 17;
    this.imageHeight = containerHeight + PLAYER_PANEL_HEIGHT;
    this.inventoryLabelY = this.imageHeight - 94;
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    int x = this.leftPos;
    int y = this.topPos;
    int containerHeight = AnePackMenu.PACK_ROWS * 18 + 17;
    graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, containerHeight);
    graphics.blit(TEXTURE, x, y + containerHeight, 0, PLAYER_PANEL_TEXTURE_Y, this.imageWidth, PLAYER_PANEL_HEIGHT);
  }
}
