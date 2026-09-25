package com.seroka.client.screen;

import com.seroka.chimere.LamaPackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 3x3 sac lama + inventaire joueur. */
public class LamaPackScreen extends AbstractContainerScreen<LamaPackMenu> {

  private static final ResourceLocation TEXTURE =
      ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

  public LamaPackScreen(LamaPackMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    this.imageHeight = 166;
    this.inventoryLabelY = this.imageHeight - 94;
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    int x = (this.width - this.imageWidth) / 2;
    int y = (this.height - this.imageHeight) / 2;
    graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
  }
}
