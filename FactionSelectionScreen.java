package com.seroka.client.screen;

import com.seroka.faction.FactionIds;
import com.seroka.network.payload.SelectFactionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Écran de sélection de faction affiché à la première connexion ou après une mort.
 */
public class FactionSelectionScreen extends Screen {

  private static final int OVERLAY_COLOR = 0xC0000000;
  private static final int BUTTON_WIDTH = 220;
  private static final int BUTTON_HEIGHT = 20;
  private static final int BUTTON_GAP = 8;

  public FactionSelectionScreen() {
    super(Component.translatable("screen.seroka.faction_selection"));
  }

  @Override
  protected void init() {
    int centerX = this.width / 2 - BUTTON_WIDTH / 2;
    int buttonCount = 3;
    int totalHeight = BUTTON_HEIGHT * buttonCount + BUTTON_GAP * (buttonCount - 1);
    int startY = this.height / 2 - totalHeight / 2;

    addRenderableWidget(Button.builder(
        Component.translatable("faction.seroka.frontalier.select"),
        button -> selectFaction(FactionIds.FRONTALIER)
    ).bounds(centerX, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

    addRenderableWidget(Button.builder(
        Component.translatable("faction.seroka.chimere.select"),
        button -> selectFaction(FactionIds.CHIMERE)
    ).bounds(centerX, startY + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT).build());

    addRenderableWidget(Button.builder(
        Component.translatable("faction.seroka.tetrasomie.select"),
        button -> selectTetrasomie()
    ).bounds(centerX, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build());
  }

  private void selectTetrasomie() {
    notifyTetrasomieWip();
    selectFaction(FactionIds.TETRASOMIE);
  }

  private void notifyTetrasomieWip() {
    if (this.minecraft != null && this.minecraft.player != null) {
      this.minecraft.player.displayClientMessage(
          Component.translatable("faction.seroka.tetrasomie.wip").withStyle(ChatFormatting.GOLD),
          true
      );
    }
  }

  private void selectFaction(String factionId) {
    PacketDistributor.sendToServer(new SelectFactionPayload(factionId));
    if (this.minecraft != null) {
      this.minecraft.setScreen(null);
    }
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    graphics.fill(0, 0, this.width, this.height, OVERLAY_COLOR);
    graphics.drawCenteredString(
        this.font,
        this.title,
        this.width / 2,
        this.height / 2 - 80,
        0xFFFFFF
    );
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override
  public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    graphics.fill(0, 0, this.width, this.height, OVERLAY_COLOR);
  }

  @Override
  public boolean isPauseScreen() {
    return true;
  }
}
