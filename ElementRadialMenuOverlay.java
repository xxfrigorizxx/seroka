package com.seroka.client;

import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import com.seroka.faction.TetrasomieSpellSlots;
import com.seroka.network.payload.SelectActiveTetrasomieSpellPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Roue tactique Tétrasomie (touche G maintenue).
 * Rendu en overlay HUD — pas d'écran Screen, pour garder la souris libre.
 */
@EventBusSubscriber(modid = com.seroka.ModMain.MODID, value = Dist.CLIENT)
public final class ElementRadialMenuOverlay {

  public static final int SLOT_COUNT = TetrasomieSpellSlots.COUNT;
  private static final double SECTOR_DEGREES = 360.0D / SLOT_COUNT;
  private static final int INNER_RADIUS = 36;
  private static final int OUTER_RADIUS = 112;
  private static final int LABEL_RADIUS = 74;
  private static final int MAX_LABEL_WIDTH = 58;
  private static final int OVERLAY_DIM = 0x88000000;
  private static final int WHEEL_COLOR = 0xCCFFFFFF;
  private static final int HOVER_COLOR = 0x66AAFFFF;
  private static final int ACTIVE_COLOR = 0x8844AAFF;
  private static final int LABEL_COLOR = 0xFFFFFFFF;
  private static final int SLOT_NUM_COLOR = 0xFFAAEEFF;
  private static final int HINT_BG = 0xCC1A2838;

  private static boolean open;
  private static boolean secondaryMenu;
  private static int lastMouseX;
  private static int lastMouseY;

  private ElementRadialMenuOverlay() {}

  public static boolean isOpen() {
    return open;
  }

  public static void open(Minecraft minecraft, boolean isSecondaryMenu) {
    if (minecraft.player == null || minecraft.screen != null) {
      return;
    }
    open = true;
    secondaryMenu = isSecondaryMenu;
    minecraft.mouseHandler.releaseMouse();
  }

  public static void closeAndSelect(Minecraft minecraft) {
    if (!open) {
      return;
    }
    open = false;
    boolean wasSecondaryMenu = secondaryMenu;
    secondaryMenu = false;

    int hoveredSlot = hoveredSlot(minecraft, lastMouseX, lastMouseY);
    if (hoveredSlot >= 0 && minecraft.player != null) {
      PlayerFaction faction = minecraft.player.getData(ModAttachments.PLAYER_FACTION);
      PacketDistributor.sendToServer(new SelectActiveTetrasomieSpellPayload(hoveredSlot, wasSecondaryMenu));
      minecraft.player.setData(
          ModAttachments.PLAYER_FACTION,
          faction.withSelectedSpellSlot(hoveredSlot, wasSecondaryMenu)
      );
    }

    if (minecraft.screen == null && minecraft.player != null) {
      minecraft.mouseHandler.grabMouse();
    }
  }

  public static void forceClose(Minecraft minecraft) {
    if (!open) {
      return;
    }
    open = false;
    secondaryMenu = false;
    if (minecraft.screen == null && minecraft.player != null) {
      minecraft.mouseHandler.grabMouse();
    }
  }

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    if (!open) {
      return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.screen != null) {
      forceClose(minecraft);
      return;
    }
    if (!minecraft.player.getData(ModAttachments.PLAYER_FACTION).isTetrasomie()) {
      forceClose(minecraft);
      return;
    }
    if (!minecraft.mouseHandler.isMouseGrabbed()) {
      return;
    }
    minecraft.mouseHandler.releaseMouse();
  }

  @SubscribeEvent
  public static void onRenderGui(RenderGuiEvent.Post event) {
    if (!open) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.screen != null) {
      return;
    }
    if (!minecraft.player.getData(ModAttachments.PLAYER_FACTION).isTetrasomie()) {
      return;
    }

    int width = event.getGuiGraphics().guiWidth();
    int height = event.getGuiGraphics().guiHeight();
    int mouseX = scaledMouseX(minecraft, width);
    int mouseY = scaledMouseY(minecraft, height);
    lastMouseX = mouseX;
    lastMouseY = mouseY;

    render(event.getGuiGraphics(), minecraft, width, height, mouseX, mouseY);
  }

  private static void render(
      GuiGraphics graphics,
      Minecraft minecraft,
      int width,
      int height,
      int mouseX,
      int mouseY
  ) {
    graphics.fill(0, 0, width, height, OVERLAY_DIM);

    int centerX = width / 2;
    int centerY = height / 2;
    int hoveredSlot = hoveredSlot(minecraft, mouseX, mouseY);
    PlayerFaction faction = minecraft.player.getData(ModAttachments.PLAYER_FACTION);
    int activeSlot = secondaryMenu ? faction.secondarySpellSlot() : faction.primarySpellSlot();

    drawWheel(graphics, centerX, centerY, hoveredSlot, activeSlot);
    drawCenterLabel(graphics, minecraft, centerX, centerY, hoveredSlot, faction);
    drawSlotLabels(graphics, minecraft, centerX, centerY, faction);
    drawBottomHint(graphics, minecraft, width, height, hoveredSlot, faction);
  }

  private static void drawCenterLabel(
      GuiGraphics graphics,
      Minecraft minecraft,
      int centerX,
      int centerY,
      int hoveredSlot,
      PlayerFaction faction
  ) {
    Component label;
    if (hoveredSlot >= 0) {
      String spellId = faction.equippedSpell(hoveredSlot);
      if (spellId.isBlank()) {
        label = Component.translatable("faction.seroka.tetrasomie.spell.empty");
      } else {
        label = Component.translatable("faction.seroka.tetrasomie.spell." + spellId);
      }
    } else {
      label = secondaryMenu
          ? Component.translatable("faction.seroka.tetrasomie.selection.secondary")
          : Component.translatable("faction.seroka.tetrasomie.selection.primary");
    }

    String texte = tronquer(minecraft, label.getString(), 140);
    int textWidth = minecraft.font.width(texte);
    graphics.drawString(minecraft.font, texte, centerX - textWidth / 2, centerY - 5, LABEL_COLOR, true);
  }

  private static void drawBottomHint(
      GuiGraphics graphics,
      Minecraft minecraft,
      int width,
      int height,
      int hoveredSlot,
      PlayerFaction faction
  ) {
    if (hoveredSlot < 0) {
      return;
    }

    String spellId = faction.equippedSpell(hoveredSlot);
    Component slotLabel = Component.translatable("faction.seroka.tetrasomie.slot", hoveredSlot + 1);
    Component spellLabel = spellId.isBlank()
        ? Component.translatable("faction.seroka.tetrasomie.spell.empty")
        : Component.translatable("faction.seroka.tetrasomie.spell." + spellId);
    String hint = slotLabel.getString() + " — " + spellLabel.getString();
    hint = tronquer(minecraft, hint, Math.min(280, width - 40));

    int hintWidth = minecraft.font.width(hint) + 16;
    int hintX = (width - hintWidth) / 2;
    int hintY = height - 36;
    graphics.fill(hintX, hintY, hintX + hintWidth, hintY + 18, HINT_BG);
    graphics.drawString(minecraft.font, hint, hintX + 8, hintY + 5, LABEL_COLOR, true);
  }

  private static int scaledMouseX(Minecraft minecraft, int guiWidth) {
    return (int) (minecraft.mouseHandler.xpos() * guiWidth / minecraft.getWindow().getScreenWidth());
  }

  private static int scaledMouseY(Minecraft minecraft, int guiHeight) {
    return (int) (minecraft.mouseHandler.ypos() * guiHeight / minecraft.getWindow().getScreenHeight());
  }

  private static int hoveredSlot(Minecraft minecraft, int mouseX, int mouseY) {
    int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
    int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;
    int dx = mouseX - centerX;
    int dy = mouseY - centerY;
    double distance = Math.sqrt((double) dx * dx + (double) dy * dy);
    if (distance < INNER_RADIUS || distance > OUTER_RADIUS) {
      return -1;
    }

    double angle = Math.toDegrees(Math.atan2(dy, dx));
    double normalized = (angle + 90.0D + 360.0D) % 360.0D;
    return (int) (normalized / SECTOR_DEGREES) % SLOT_COUNT;
  }

  private static void drawWheel(GuiGraphics graphics, int centerX, int centerY, int hoveredSlot, int activeSlot) {
    for (int slot = 0; slot < SLOT_COUNT; slot++) {
      double start = sectorStartAngle(slot);
      double end = start + SECTOR_DEGREES;
      if (slot == hoveredSlot) {
        fillSector(graphics, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, start, end, HOVER_COLOR);
      } else if (slot == activeSlot) {
        fillSector(graphics, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, start, end, ACTIVE_COLOR);
      }
      drawSectorBorder(graphics, centerX, centerY, OUTER_RADIUS, start, end, WHEEL_COLOR);
      drawSectorBorder(graphics, centerX, centerY, INNER_RADIUS, start, end, WHEEL_COLOR);
    }
    drawCircleOutline(graphics, centerX, centerY, OUTER_RADIUS, WHEEL_COLOR, 2);
    drawCircleOutline(graphics, centerX, centerY, INNER_RADIUS, WHEEL_COLOR, 2);
  }

  private static void drawSlotLabels(
      GuiGraphics graphics,
      Minecraft minecraft,
      int centerX,
      int centerY,
      PlayerFaction faction
  ) {
    for (int slot = 0; slot < SLOT_COUNT; slot++) {
      double midAngle = Math.toRadians(sectorStartAngle(slot) + SECTOR_DEGREES / 2.0D);
      int labelX = centerX + (int) Math.round(Math.cos(midAngle) * LABEL_RADIUS);
      int labelY = centerY + (int) Math.round(Math.sin(midAngle) * LABEL_RADIUS);

      String spellId = faction.equippedSpell(slot);
      Component label = spellId.isBlank()
          ? Component.translatable("faction.seroka.tetrasomie.spell.empty")
          : Component.translatable("faction.seroka.tetrasomie.spell." + spellId);

      String numero = String.valueOf(slot + 1);
      String nomCourt = tronquer(minecraft, label.getString(), MAX_LABEL_WIDTH);

      graphics.drawCenteredString(minecraft.font, numero, labelX, labelY - 8, SLOT_NUM_COLOR);
      graphics.drawCenteredString(minecraft.font, nomCourt, labelX, labelY + 2, LABEL_COLOR);
    }
  }

  private static String tronquer(Minecraft minecraft, String texte, int largeurMax) {
    if (minecraft.font.width(texte) <= largeurMax) {
      return texte;
    }
    String ellipsis = "…";
    return minecraft.font.plainSubstrByWidth(texte, largeurMax - minecraft.font.width(ellipsis)) + ellipsis;
  }

  private static double sectorStartAngle(int slot) {
    return -90.0D + slot * SECTOR_DEGREES;
  }

  private static void fillSector(
      GuiGraphics graphics,
      int centerX,
      int centerY,
      int innerRadius,
      int outerRadius,
      double startDegrees,
      double endDegrees,
      int color
  ) {
    for (int radius = innerRadius; radius <= outerRadius; radius++) {
      drawArc(graphics, centerX, centerY, radius, startDegrees, endDegrees, color, 2);
    }
  }

  private static void drawSectorBorder(
      GuiGraphics graphics,
      int centerX,
      int centerY,
      int radius,
      double startDegrees,
      double endDegrees,
      int color
  ) {
    drawArc(graphics, centerX, centerY, radius, startDegrees, endDegrees, color, 1);
    drawRadiusLine(graphics, centerX, centerY, radius, startDegrees, color);
  }

  private static void drawRadiusLine(
      GuiGraphics graphics,
      int centerX,
      int centerY,
      int radius,
      double degrees,
      int color
  ) {
    double radians = Math.toRadians(degrees);
    int x = centerX + (int) Math.round(Math.cos(radians) * radius);
    int y = centerY + (int) Math.round(Math.sin(radians) * radius);
    graphics.fill(centerX, centerY, x + 1, y + 1, color);
  }

  private static void drawArc(
      GuiGraphics graphics,
      int centerX,
      int centerY,
      int radius,
      double startDegrees,
      double endDegrees,
      int color,
      int thickness
  ) {
    for (double angle = startDegrees; angle <= endDegrees; angle += 1.0D) {
      double radians = Math.toRadians(angle);
      int x = centerX + (int) Math.round(Math.cos(radians) * radius);
      int y = centerY + (int) Math.round(Math.sin(radians) * radius);
      graphics.fill(x, y, x + thickness, y + thickness, color);
    }
  }

  private static void drawCircleOutline(
      GuiGraphics graphics,
      int centerX,
      int centerY,
      int radius,
      int color,
      int thickness
  ) {
    drawArc(graphics, centerX, centerY, radius, 0.0D, 360.0D, color, thickness);
  }
}
