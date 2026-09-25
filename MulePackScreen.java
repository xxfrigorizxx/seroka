package com.seroka.client.screen;

import com.seroka.chimere.MulePackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 3x4 sac de bât + inventaire joueur (texture distributeur, sans slots fantômes). */
public class MulePackScreen extends AbstractContainerScreen<MulePackMenu> {

  private static final ResourceLocation DISPENSER_TEXTURE =
      ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");
  private static final ResourceLocation CHEST_TEXTURE =
      ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

  private static final int HEADER_HEIGHT = 17;
  private static final int ROW_HEIGHT = 18;
  private static final int PLAYER_PANEL_HEIGHT = 96;
  private static final int PLAYER_PANEL_TEXTURE_Y = 126;
  private static final int PACK_SLOT_AREA_TOP = 17;
  private static final int DISPENSER_SLOT_ROWS = 3;
  private static final int RIGHT_PANEL_X = MulePackMenu.PACK_START_X + MulePackMenu.PACK_SLOT_WIDTH;

  public MulePackScreen(MulePackMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    int packHeight = HEADER_HEIGHT + MulePackMenu.PACK_ROWS * ROW_HEIGHT;
    this.imageHeight = packHeight + PLAYER_PANEL_HEIGHT;
    this.inventoryLabelY = this.imageHeight - 94;
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    int x = this.leftPos;
    int y = this.topPos;
    int packHeight = HEADER_HEIGHT + MulePackMenu.PACK_ROWS * ROW_HEIGHT;

    graphics.blit(DISPENSER_TEXTURE, x, y, 0, 0, this.imageWidth, HEADER_HEIGHT);

    for (int row = 0; row < MulePackMenu.PACK_ROWS; row++) {
      int screenY = y + PACK_SLOT_AREA_TOP + row * ROW_HEIGHT;
      if (row < DISPENSER_SLOT_ROWS) {
        int textureY = PACK_SLOT_AREA_TOP + row * ROW_HEIGHT;
        graphics.blit(DISPENSER_TEXTURE, x, screenY, 0, textureY, this.imageWidth, ROW_HEIGHT);
        continue;
      }

      graphics.blit(
          DISPENSER_TEXTURE,
          x,
          screenY,
          0,
          PACK_SLOT_AREA_TOP,
          MulePackMenu.PACK_START_X,
          ROW_HEIGHT
      );
      int textureY = PACK_SLOT_AREA_TOP + row * ROW_HEIGHT;
      graphics.blit(
          CHEST_TEXTURE,
          x + MulePackMenu.PACK_START_X,
          screenY,
          MulePackMenu.PACK_START_X,
          textureY,
          MulePackMenu.PACK_SLOT_WIDTH,
          ROW_HEIGHT
      );
      graphics.blit(
          DISPENSER_TEXTURE,
          x + RIGHT_PANEL_X,
          screenY,
          RIGHT_PANEL_X,
          PACK_SLOT_AREA_TOP,
          this.imageWidth - RIGHT_PANEL_X,
          ROW_HEIGHT
      );
    }

    graphics.blit(
        CHEST_TEXTURE,
        x,
        y + packHeight,
        0,
        PLAYER_PANEL_TEXTURE_Y,
        this.imageWidth,
        PLAYER_PANEL_HEIGHT
    );
  }
}
