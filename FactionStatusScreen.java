package com.seroka.client.screen;

import com.seroka.faction.ArmureEauAllieCastHelper;
import com.seroka.faction.IcePrisonCastHelper;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import com.seroka.faction.TetrasomieElements;
import com.seroka.faction.TetrasomieProgressionHelper;
import com.seroka.faction.TetrasomieSpellCatalog;
import com.seroka.faction.TetrasomieSpellSlots;
import com.seroka.network.payload.EquipTetrasomieSpellPayload;
import com.seroka.tetrasomie.TetrasomieSpells;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Fiche Tétrasomie (touche K) : statut + équipement des sorts. */
public class FactionStatusScreen extends Screen {

  private static final int OVERLAY_COLOR = 0xC0000000;
  private static final int PANEL_BG_COLOR = 0xC0282828;
  private static final int BORDER_COLOR = 0xFFFFFFFF;
  private static final int TEXT_COLOR = 0xFFFFFFFF;
  private static final int STATUS_COLOR = 0xFF66CCFF;
  private static final int MUTED_COLOR = 0xFFAAAAAA;
  private static final int XP_BAR_BG = 0xFF1A1A1A;
  private static final int XP_BAR_FILL = 0xFF33CCFF;
  private static final int SLOT_BG = 0xFF3A3A3A;
  private static final int SLOT_SELECTED = 0xFF5599FF;
  private static final int SLOT_ACTIVE = 0xFF3377AA;
  private static final int SCROLL_TRACK = 0xFF1A1A1A;
  private static final int SCROLL_THUMB = 0xFF66AAFF;

  private static final int PANEL_WIDTH = 400;
  private static final int PANEL_HEIGHT = 288;
  private static final int PANEL_PADDING = 14;
  private static final int NAV_BTN_WIDTH = 96;
  private static final int BUTTON_HEIGHT = 20;
  private static final int FOOTER_GAP = 10;
  private static final int XP_BAR_WIDTH = 320;
  private static final int XP_BAR_HEIGHT = 10;

  private static final int CONTENT_WIDTH = PANEL_WIDTH - PANEL_PADDING * 2;
  private static final int SPELL_COLS = 2;
  private static final int SPELL_COL_GAP = 8;
  private static final int SPELL_BTN_WIDTH = (CONTENT_WIDTH - SPELL_COL_GAP) / SPELL_COLS;
  private static final int SPELL_ROW_HEIGHT = 24;
  private static final int SPELL_LIST_HEIGHT = 108;
  private static final int STATUS_SPELL_LINE_HEIGHT = 20;
  private static final int STATUS_SPELL_LIST_HEIGHT = 104;
  private static final int SLOT_COLS = 3;
  private static final int SLOT_GAP = 8;
  private static final int SLOT_BTN_WIDTH = (CONTENT_WIDTH - SLOT_GAP * (SLOT_COLS - 1)) / SLOT_COLS;
  private static final int SLOT_LABEL_HEIGHT = 14;
  private static final int SLOT_ROW_HEIGHT = BUTTON_HEIGHT + SLOT_LABEL_HEIGHT + 6;

  private enum Page {
    STATUS,
    SPELLS
  }

  private Page currentPage = Page.STATUS;
  private String selectedSpellId;
  private int selectedEquipSlot = -1;
  private double spellScrollOffset;
  private double statusScrollOffset;

  public FactionStatusScreen() {
    super(Component.translatable("screen.seroka.faction_status"));
  }

  @Override
  protected void init() {
    refreshLayout();
  }

  private void refreshLayout() {
    clearWidgets();
    int panelX = panelX();
    int panelY = panelY();
    int bottomY = footerTopY(panelY);

    addRenderableWidget(Button.builder(
        Component.translatable("faction.seroka.tetrasomie.page.status"),
        button -> switchPage(Page.STATUS)
    ).bounds(panelX + PANEL_PADDING, bottomY, NAV_BTN_WIDTH, BUTTON_HEIGHT).build());

    addRenderableWidget(Button.builder(
        Component.translatable("faction.seroka.tetrasomie.page.spells"),
        button -> switchPage(Page.SPELLS)
    ).bounds(panelX + PANEL_PADDING + NAV_BTN_WIDTH + 8, bottomY, NAV_BTN_WIDTH, BUTTON_HEIGHT).build());

    addRenderableWidget(Button.builder(
        Component.translatable("gui.done"),
        button -> onClose()
    ).bounds(panelX + PANEL_WIDTH - PANEL_PADDING - NAV_BTN_WIDTH, bottomY, NAV_BTN_WIDTH, BUTTON_HEIGHT).build());

    if (currentPage == Page.SPELLS) {
      initSpellPageWidgets(panelX, panelY);
    }
  }

  private int footerTopY(int panelY) {
    return panelY + PANEL_HEIGHT - PANEL_PADDING - BUTTON_HEIGHT;
  }

  private void switchPage(Page page) {
    if (currentPage != page) {
      spellScrollOffset = 0.0D;
      statusScrollOffset = 0.0D;
    }
    currentPage = page;
    refreshLayout();
  }

  private void initSpellPageWidgets(int panelX, int panelY) {
    int listTop = spellListTop(panelY);
    int listLeft = panelX + PANEL_PADDING;
    spellScrollOffset = Mth.clamp(spellScrollOffset, 0.0D, maxSpellScroll());

    PlayerFaction faction = currentFaction();
    List<String> unlocked = TetrasomieSpellCatalog.getUnlockedSpellIds(faction);
    for (int index = 0; index < unlocked.size(); index++) {
      String spellId = unlocked.get(index);
      int col = index % SPELL_COLS;
      int row = index / SPELL_COLS;
      int spellX = listLeft + col * (SPELL_BTN_WIDTH + SPELL_COL_GAP);
      int spellY = listTop + row * SPELL_ROW_HEIGHT - (int) spellScrollOffset;
      int spellBottom = spellY + BUTTON_HEIGHT;
      if (spellBottom < listTop || spellY > listTop + SPELL_LIST_HEIGHT) {
        continue;
      }

      Component labelComplet = Component.translatable(spellLabelKey(spellId));
      String labelCourt = tronquer(labelComplet.getString(), SPELL_BTN_WIDTH - 12);
      Button spellButton = Button.builder(
          Component.literal(labelCourt),
          button -> {
            selectedSpellId = spellId;
            selectedEquipSlot = -1;
            refreshLayout();
          }
      ).bounds(spellX, spellY, SPELL_BTN_WIDTH, BUTTON_HEIGHT).build();
      spellButton.setTooltip(Tooltip.create(labelComplet));
      addRenderableWidget(spellButton);
    }

    int slotsTop = slotsSectionTop(panelY);
    for (int slot = 0; slot < TetrasomieSpellSlots.COUNT; slot++) {
      int row = slot / SLOT_COLS;
      int col = slot % SLOT_COLS;
      int slotX = listLeft + col * (SLOT_BTN_WIDTH + SLOT_GAP);
      int slotY = slotsTop + row * SLOT_ROW_HEIGHT;
      int capturedSlot = slot;
      addRenderableWidget(Button.builder(
          Component.translatable("faction.seroka.tetrasomie.slot", slot + 1),
          button -> assignSelectedSpellToSlot(capturedSlot)
      ).bounds(slotX, slotY, SLOT_BTN_WIDTH, BUTTON_HEIGHT).build());
    }
  }

  private String tronquer(String texte, int largeurMax) {
    if (this.font.width(texte) <= largeurMax) {
      return texte;
    }
    String ellipsis = "…";
    return this.font.plainSubstrByWidth(texte, largeurMax - this.font.width(ellipsis)) + ellipsis;
  }

  private void assignSelectedSpellToSlot(int slot) {
    if (selectedSpellId == null || selectedSpellId.isBlank()) {
      selectedEquipSlot = slot;
      refreshLayout();
      return;
    }
    PacketDistributor.sendToServer(new EquipTetrasomieSpellPayload(selectedSpellId, slot));
    if (this.minecraft != null && this.minecraft.player != null) {
      PlayerFaction updated = currentFaction()
          .withEquippedSpell(slot, selectedSpellId)
          .withSelectedSpellSlot(slot, false);
      this.minecraft.player.setData(ModAttachments.PLAYER_FACTION, updated);
    }
    selectedSpellId = null;
    selectedEquipSlot = slot;
    refreshLayout();
  }

  private PlayerFaction currentFaction() {
    if (this.minecraft == null || this.minecraft.player == null) {
      return PlayerFaction.defaultValue();
    }
    return this.minecraft.player.getData(ModAttachments.PLAYER_FACTION);
  }

  private Component specializationLine(PlayerFaction faction) {
    String element = faction.getElement();
    Component specialization;
    if (TetrasomieElements.EAU.equals(element)) {
      specialization = Component.translatable("faction.seroka.tetrasomie.specialization.eau");
    } else if (TetrasomieElements.TERRE.equals(element)) {
      specialization = Component.translatable("faction.seroka.tetrasomie.specialization.terre");
    } else if (TetrasomieElements.FEU.equals(element)) {
      specialization = Component.translatable("faction.seroka.tetrasomie.specialization.feu");
    } else if (TetrasomieElements.AIR.equals(element)) {
      specialization = Component.translatable("faction.seroka.tetrasomie.specialization.air");
    } else {
      specialization = Component.translatable("faction.seroka.tetrasomie.specialization.unknown");
    }
    return Component.translatable("faction.seroka.tetrasomie.status.line", specialization);
  }

  private static String spellLabelKey(String spellId) {
    return "faction.seroka.tetrasomie.spell." + spellId;
  }

  private int panelX() {
    return (this.width - PANEL_WIDTH) / 2;
  }

  private int panelY() {
    return (this.height - PANEL_HEIGHT) / 2;
  }

  private int statusListTop(int panelY) {
    return panelY + PANEL_PADDING + 112;
  }

  private double maxStatusScroll() {
    List<String> unlocked = TetrasomieSpellCatalog.getUnlockedSpellIds(currentFaction());
    int contentHeight = unlocked.size() * STATUS_SPELL_LINE_HEIGHT;
    return Math.max(0, contentHeight - STATUS_SPELL_LIST_HEIGHT);
  }

  private boolean isMouseOverStatusSpellList(double mouseX, double mouseY) {
    int listTop = statusListTop(panelY());
    int listLeft = panelX() + PANEL_PADDING;
    return mouseX >= listLeft
        && mouseX < listLeft + CONTENT_WIDTH
        && mouseY >= listTop
        && mouseY < listTop + STATUS_SPELL_LIST_HEIGHT;
  }

  private Component spellStatLine(PlayerFaction faction, String spellId) {
    if (TetrasomieSpells.WATER_BALL.equals(spellId)) {
      return Component.translatable(
          "faction.seroka.tetrasomie.water_ball.range",
          (int) TetrasomieProgressionHelper.getWaterBallMaxRange(faction)
      );
    }
    if (TetrasomieSpells.ICE_SPIKE.equals(spellId)) {
      return Component.translatable(
          "faction.seroka.tetrasomie.ice_spike.range",
          (int) TetrasomieProgressionHelper.getIceSpikeMaxRange(faction)
      );
    }
    if (TetrasomieSpells.WATER_HEAL_ALLY.equals(spellId)) {
      return Component.translatable(
          "faction.seroka.tetrasomie.water_heal_ally.range",
          (int) TetrasomieProgressionHelper.WATER_HEAL_ALLY_RANGE
      );
    }
    if (TetrasomieSpells.PRISON_GLACE.equals(spellId)
        || TetrasomieSpells.GEYSER_OFFENSIF.equals(spellId)
        || TetrasomieSpells.ICE_CRUSH.equals(spellId)) {
      return Component.translatable(
          "faction.seroka.tetrasomie.spell.range_hitscan",
          (int) IcePrisonCastHelper.PORTEE_BLOCS
      );
    }
    if (TetrasomieSpells.ARMURE_EAU_ALLIE.equals(spellId)) {
      return Component.translatable(
          "faction.seroka.tetrasomie.spell.range_hitscan",
          (int) ArmureEauAllieCastHelper.PORTEE_CIBLAGE
      );
    }
    return Component.translatable("faction.seroka.tetrasomie.spell.stat." + spellId);
  }

  private int spellListTop(int panelY) {
    return panelY + PANEL_PADDING + 38;
  }

  private int slotsSectionTop(int panelY) {
    int base = spellListTop(panelY) + SPELL_LIST_HEIGHT + 22;
    if (selectedSpellId != null) {
      base += 14;
    }
    return base;
  }

  private double maxSpellScroll() {
    List<String> unlocked = TetrasomieSpellCatalog.getUnlockedSpellIds(currentFaction());
    int rows = (unlocked.size() + SPELL_COLS - 1) / SPELL_COLS;
    int contentHeight = rows * SPELL_ROW_HEIGHT;
    return Math.max(0, contentHeight - SPELL_LIST_HEIGHT);
  }

  private boolean isMouseOverSpellList(double mouseX, double mouseY) {
    int listTop = spellListTop(panelY());
    int listLeft = panelX() + PANEL_PADDING;
    return mouseX >= listLeft
        && mouseX < listLeft + CONTENT_WIDTH
        && mouseY >= listTop
        && mouseY < listTop + SPELL_LIST_HEIGHT;
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    if (currentPage == Page.STATUS && isMouseOverStatusSpellList(mouseX, mouseY)) {
      statusScrollOffset = Mth.clamp(
          statusScrollOffset - scrollY * STATUS_SPELL_LINE_HEIGHT,
          0.0D,
          maxStatusScroll()
      );
      return true;
    }
    if (currentPage == Page.SPELLS && isMouseOverSpellList(mouseX, mouseY)) {
      spellScrollOffset = Mth.clamp(spellScrollOffset - scrollY * SPELL_ROW_HEIGHT, 0.0D, maxSpellScroll());
      refreshLayout();
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  @Override
  public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    graphics.fill(0, 0, this.width, this.height, OVERLAY_COLOR);
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    super.render(graphics, mouseX, mouseY, partialTick);

    int panelX = panelX();
    int panelY = panelY();
    int textX = panelX + PANEL_PADDING;

    drawPanel(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
    graphics.fill(
        panelX + PANEL_PADDING,
        footerTopY(panelY) - FOOTER_GAP,
        panelX + PANEL_WIDTH - PANEL_PADDING,
        footerTopY(panelY) - FOOTER_GAP + 1,
        0xFF555555
    );

    graphics.drawString(this.font, this.title, textX, panelY + PANEL_PADDING, TEXT_COLOR, false);

    if (currentPage == Page.STATUS) {
      renderStatusPage(graphics, textX, panelY, mouseX, mouseY);
    } else {
      renderSpellsPage(graphics, textX, panelY, mouseX, mouseY);
    }
  }

  private void renderStatusPage(GuiGraphics graphics, int textX, int panelY, int mouseX, int mouseY) {
    PlayerFaction faction = currentFaction();
    int y = panelY + PANEL_PADDING + 18;

    graphics.drawString(this.font, specializationLine(faction), textX, y, STATUS_COLOR, false);
    y += 16;
    graphics.drawString(
        this.font,
        Component.translatable("faction.seroka.tetrasomie.level", faction.elementLevel()),
        textX,
        y,
        TEXT_COLOR,
        false
    );
    y += 14;

    int barX = textX;
    int barY = y;
    graphics.fill(barX, barY, barX + XP_BAR_WIDTH, barY + XP_BAR_HEIGHT, XP_BAR_BG);
    int fillWidth = Math.round(XP_BAR_WIDTH * faction.elementXpProgress());
    if (fillWidth > 0) {
      graphics.fill(barX, barY, barX + fillWidth, barY + XP_BAR_HEIGHT, XP_BAR_FILL);
    }
    graphics.renderOutline(barX, barY, XP_BAR_WIDTH, XP_BAR_HEIGHT, BORDER_COLOR);

    y += XP_BAR_HEIGHT + 6;
    graphics.drawString(
        this.font,
        Component.translatable(
            "faction.seroka.tetrasomie.xp_progress",
            (int) faction.elementXp(),
            (int) faction.elementXpRequiredForNextLevel()
        ),
        textX,
        y,
        MUTED_COLOR,
        false
    );

    int listTop = statusListTop(panelY);
    int listBottom = listTop + STATUS_SPELL_LIST_HEIGHT;
    graphics.drawString(
        this.font,
        Component.translatable("faction.seroka.tetrasomie.spells.unlocked"),
        textX,
        listTop - 12,
        TEXT_COLOR,
        false
    );

    List<String> unlocked = TetrasomieSpellCatalog.getUnlockedSpellIds(faction);
    statusScrollOffset = Mth.clamp(statusScrollOffset, 0.0D, maxStatusScroll());

    if (unlocked.isEmpty()) {
      graphics.drawString(
          this.font,
          Component.translatable("faction.seroka.tetrasomie.spells.none"),
          textX,
          listTop + 4,
          MUTED_COLOR,
          false
      );
      return;
    }

    graphics.fill(textX, listTop, textX + CONTENT_WIDTH, listBottom, 0x66000000);
    graphics.renderOutline(textX, listTop, CONTENT_WIDTH, STATUS_SPELL_LIST_HEIGHT, BORDER_COLOR);
    graphics.enableScissor(textX, listTop, textX + CONTENT_WIDTH, listBottom);

    for (int index = 0; index < unlocked.size(); index++) {
      String spellId = unlocked.get(index);
      int lineY = listTop + index * STATUS_SPELL_LINE_HEIGHT - (int) statusScrollOffset;
      if (lineY + STATUS_SPELL_LINE_HEIGHT < listTop || lineY > listBottom) {
        continue;
      }
      String nom = tronquer(Component.translatable(spellLabelKey(spellId)).getString(), CONTENT_WIDTH - 12);
      graphics.drawString(this.font, nom, textX + 4, lineY + 1, STATUS_COLOR, false);
      String statText = tronquer(spellStatLine(faction, spellId).getString(), CONTENT_WIDTH - 12);
      graphics.drawString(this.font, statText, textX + 4, lineY + 11, MUTED_COLOR, false);
    }

    graphics.disableScissor();
    drawScrollBar(graphics, textX, listTop, STATUS_SPELL_LIST_HEIGHT, statusScrollOffset, maxStatusScroll());

    if (isMouseOverStatusSpellList(mouseX, mouseY)) {
      graphics.drawString(
          this.font,
          Component.translatable("faction.seroka.tetrasomie.spells.scroll_hint"),
          textX,
          listBottom + 4,
          MUTED_COLOR,
          false
      );
    }
  }

  private void renderSpellsPage(GuiGraphics graphics, int textX, int panelY, int mouseX, int mouseY) {
    PlayerFaction faction = currentFaction();
    int y = panelY + PANEL_PADDING + 18;

    graphics.drawString(
        this.font,
        Component.translatable("faction.seroka.tetrasomie.spells.unlocked"),
        textX,
        y,
        TEXT_COLOR,
        false
    );

    List<String> unlocked = TetrasomieSpellCatalog.getUnlockedSpellIds(faction);
    int listTop = spellListTop(panelY);
    int listLeft = textX;
    int listBottom = listTop + SPELL_LIST_HEIGHT;

    if (unlocked.isEmpty()) {
      graphics.drawString(
          this.font,
          Component.translatable("faction.seroka.tetrasomie.spells.none"),
          textX,
          listTop + 4,
          MUTED_COLOR,
          false
      );
    } else {
      graphics.fill(listLeft, listTop, listLeft + CONTENT_WIDTH, listBottom, 0x66000000);
      graphics.renderOutline(listLeft, listTop, CONTENT_WIDTH, SPELL_LIST_HEIGHT, BORDER_COLOR);

      graphics.enableScissor(listLeft, listTop, listLeft + CONTENT_WIDTH, listBottom);
      for (int index = 0; index < unlocked.size(); index++) {
        String spellId = unlocked.get(index);
        if (!spellId.equals(selectedSpellId)) {
          continue;
        }
        int col = index % SPELL_COLS;
        int row = index / SPELL_COLS;
        int spellX = listLeft + col * (SPELL_BTN_WIDTH + SPELL_COL_GAP);
        int spellY = listTop + row * SPELL_ROW_HEIGHT - (int) spellScrollOffset;
        if (spellY + BUTTON_HEIGHT >= listTop && spellY <= listBottom) {
          graphics.fill(spellX - 2, spellY - 2, spellX + SPELL_BTN_WIDTH + 2, spellY + BUTTON_HEIGHT + 2, SLOT_SELECTED);
        }
      }
      graphics.disableScissor();

      drawScrollBar(graphics, listLeft, listTop, SPELL_LIST_HEIGHT, spellScrollOffset, maxSpellScroll());

      if (isMouseOverSpellList(mouseX, mouseY)) {
        graphics.drawString(
            this.font,
            Component.translatable("faction.seroka.tetrasomie.spells.scroll_hint"),
            textX,
            listBottom + 4,
            MUTED_COLOR,
            false
        );
      }
    }

    if (selectedSpellId != null) {
      graphics.drawString(
          this.font,
          Component.translatable("faction.seroka.tetrasomie.spells.assign_hint"),
          textX,
          listBottom + 6,
          MUTED_COLOR,
          false
      );
    }

    int slotsLabelY = slotsSectionTop(panelY) - 12;
    graphics.drawString(
        this.font,
        Component.translatable("faction.seroka.tetrasomie.spells.equip_slots"),
        textX,
        slotsLabelY,
        TEXT_COLOR,
        false
    );

    int slotsTop = slotsSectionTop(panelY);
    int activeSlot = faction.primarySpellSlot();
    for (int slot = 0; slot < TetrasomieSpellSlots.COUNT; slot++) {
      int row = slot / SLOT_COLS;
      int col = slot % SLOT_COLS;
      int slotX = textX + col * (SLOT_BTN_WIDTH + SLOT_GAP);
      int slotY = slotsTop + row * SLOT_ROW_HEIGHT;
      int labelY = slotY + BUTTON_HEIGHT + 2;

      int color;
      if (slot == selectedEquipSlot) {
        color = SLOT_SELECTED;
      } else if (slot == activeSlot) {
        color = SLOT_ACTIVE;
      } else {
        color = SLOT_BG;
      }
      graphics.fill(slotX, labelY, slotX + SLOT_BTN_WIDTH, labelY + SLOT_LABEL_HEIGHT + 2, color);

      Component label = faction.equippedSpell(slot).isBlank()
          ? Component.translatable("faction.seroka.tetrasomie.spell.empty")
          : Component.translatable(spellLabelKey(faction.equippedSpell(slot)));
      String labelText = tronquer(label.getString(), SLOT_BTN_WIDTH - 6);
      graphics.drawCenteredString(this.font, labelText, slotX + SLOT_BTN_WIDTH / 2, labelY + 3, TEXT_COLOR);
    }
  }

  private void drawScrollBar(
      GuiGraphics graphics,
      int listLeft,
      int listTop,
      int listHeight,
      double scrollOffset,
      double maxScroll
  ) {
    if (maxScroll <= 0.0D) {
      return;
    }
    int trackX = listLeft + CONTENT_WIDTH - 5;
    int trackHeight = listHeight - 4;
    int thumbHeight = Math.max(12, (int) (trackHeight * (listHeight / (maxScroll + listHeight))));
    int thumbY = listTop + 2 + (int) ((trackHeight - thumbHeight) * (scrollOffset / maxScroll));
    graphics.fill(trackX, listTop + 2, trackX + 3, listTop + 2 + trackHeight, SCROLL_TRACK);
    graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, SCROLL_THUMB);
  }

  private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
    graphics.fill(x, y, x + width, y + height, PANEL_BG_COLOR);
    graphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
    graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
    graphics.fill(x, y, x + 1, y + height, BORDER_COLOR);
    graphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
