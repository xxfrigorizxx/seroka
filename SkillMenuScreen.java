package com.seroka.client.screen;

import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import com.seroka.faction.SkillStat;
import com.seroka.faction.UniqueSkillsHelper;
import com.seroka.network.payload.SpendSkillPointPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Menu de dépense des points de compétence (style SAO, rendu procédural).
 */
public class SkillMenuScreen extends Screen {

  private static final int OVERLAY_COLOR = 0xC0000000;
  private static final int PANEL_BG_COLOR = 0xC0282828;
  private static final int BORDER_COLOR = 0xFFFFFFFF;
  private static final int TEXT_COLOR = 0xFFFFFFFF;
  private static final int LEVEL_COLOR = 0xFF33FF99;
  private static final int BUTTON_BG_COLOR = 0xFF2A2A2A;
  private static final int BUTTON_HOVER_COLOR = 0xFF3A3A3A;
  private static final int BUTTON_DISABLED_COLOR = 0xFF1A1A1A;
  private static final int UNIQUE_UNLOCKED_COLOR = 0xFF33FFCC;
  private static final int DESC_COLOR = 0xFFCCCCCC;

  private static final int PANEL_WIDTH = 248;
  private static final int HEADER_HEIGHT = 44;
  private static final int ROW_HEIGHT = 20;
  private static final int ROW_SPACING = 4;
  private static final int PANEL_MARGIN = 16;
  private static final int LINE_HEIGHT = 9;

  private static final List<String> DUAL_BLADES_DESCRIPTION = List.of(
      "• Lance les deux lames en même temps (molette)",
      "• Enchaînement : slash droit → gauche → croisé",
      "• Temps d'attaque réduit de 50%"
  );
  private static final List<String> HAWKEYE_DESCRIPTION = List.of(
      "• Débloque l'utilisation de l'arc",
      "• Arbalètes et projectiles restent interdits",
      "• Compétence unique de précision à distance"
  );
  private static final List<String> LAKE_MASTER_DESCRIPTION = List.of(
      "• Débloque l'utilisation du trident",
      "• Double l'oxygène max en tenant un trident",
      "• Compétence unique aquatique"
  );
  private static final List<String> FRANC_TIREUR_DESCRIPTION = List.of(
      "• Débloque l'utilisation de l'arbalète",
      "• Seule arme à distance mécanique autorisée",
      "• Compétence unique de tir embusqué"
  );
  private static final List<String> SHURIKEN_DESCRIPTION = List.of(
      "• Obtient la Dague Infinie (liée à ton âme)",
      "• Lance la dague sans jamais la perdre",
      "• Tue 50 monstres / jour uniquement à la lame lancée"
  );
  private final PlayerFaction initialFaction;
  private int cachedSkillPoints = -1;

  public SkillMenuScreen(PlayerFaction faction) {
    super(Component.literal("Compétences"));
    this.initialFaction = faction;
  }

  @Override
  protected void init() {
    rebuildButtons();
  }

  private void rebuildButtons() {
    clearWidgets();

    PlayerFaction faction = getCurrentFaction();
    cachedSkillPoints = faction.skillPoints();
    boolean canSpend = faction.skillPoints() > 0;

    int panelX = panelX();
    int panelY = panelY();
    int buttonWidth = 132;
    int buttonX = panelX + 12;
    int firstRowY = panelY + HEADER_HEIGHT + 8;

    addStatButton(buttonX, firstRowY, buttonWidth, "[ FORCE ]", SkillStat.STR, canSpend);
    addStatButton(buttonX, firstRowY + ROW_HEIGHT + ROW_SPACING, buttonWidth, "[ VITALITÉ ]", SkillStat.VIT, canSpend);
    addStatButton(buttonX, firstRowY + 2 * (ROW_HEIGHT + ROW_SPACING), buttonWidth, "[ ENDURANCE ]", SkillStat.END, canSpend);
    addStatButton(buttonX, firstRowY + 3 * (ROW_HEIGHT + ROW_SPACING), buttonWidth, "[ AGILITÉ ]", SkillStat.AGI, canSpend);
    addStatButton(buttonX, firstRowY + 4 * (ROW_HEIGHT + ROW_SPACING), buttonWidth, "[ STAMINA ]", SkillStat.STAMINA, canSpend);
    addStatButton(buttonX, firstRowY + 5 * (ROW_HEIGHT + ROW_SPACING), buttonWidth, "[ OXYGÈNE ]", SkillStat.OXYGEN, canSpend);
  }

  private void addStatButton(int x, int y, int width, String label, SkillStat stat, boolean active) {
    addRenderableWidget(new SaoStatButton(x, y, width, ROW_HEIGHT, Component.literal(label), active, () -> spendPoint(stat)));
  }

  private void spendPoint(SkillStat stat) {
    PlayerFaction faction = getCurrentFaction();
    if (faction.skillPoints() <= 0) {
      return;
    }
    PacketDistributor.sendToServer(new SpendSkillPointPayload(stat.ordinal()));
  }

  private PlayerFaction getCurrentFaction() {
    if (this.minecraft != null && this.minecraft.player != null) {
      return this.minecraft.player.getData(ModAttachments.PLAYER_FACTION);
    }
    return initialFaction;
  }

  private int statsSectionHeight() {
    return HEADER_HEIGHT + 8 + 6 * ROW_HEIGHT + 5 * ROW_SPACING + 8;
  }

  private int uniqueSectionHeight(PlayerFaction faction) {
    if (!UniqueSkillsHelper.hasAnyUniqueSkill(faction)) {
      return 0;
    }
    return 10 + 10 + 10 + uniqueSkillDescription(faction).size() * LINE_HEIGHT + 6;
  }

  private int panelX() {
    return (this.width - PANEL_WIDTH) / 2;
  }

  private int panelY() {
    int height = panelHeight();
    return Math.max(PANEL_MARGIN, (this.height - height) / 2);
  }

  private int panelHeight() {
    PlayerFaction faction = getCurrentFaction();
    int desired = statsSectionHeight() + uniqueSectionHeight(faction);
    return Math.min(desired, this.height - PANEL_MARGIN * 2);
  }

  @Override
  public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    graphics.fill(0, 0, this.width, this.height, OVERLAY_COLOR);
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    this.renderBackground(graphics, mouseX, mouseY, partialTick);

    PlayerFaction faction = getCurrentFaction();
    int panelX = panelX();
    int panelY = panelY();
    int panelHeight = panelHeight();

    drawPanel(graphics, panelX, panelY, PANEL_WIDTH, panelHeight);
    graphics.drawString(this.font, "COMPÉTENCES", panelX + 12, panelY + 10, TEXT_COLOR, false);
    graphics.drawString(
        this.font,
        "Points : " + faction.skillPoints(),
        panelX + 12,
        panelY + 24,
        TEXT_COLOR,
        false
    );

    int firstRowY = panelY + HEADER_HEIGHT + 8;
    drawLevelLabel(graphics, panelX, firstRowY, faction.strLevel());
    drawLevelLabel(graphics, panelX, firstRowY + ROW_HEIGHT + ROW_SPACING, faction.vitLevel());
    drawLevelLabel(graphics, panelX, firstRowY + 2 * (ROW_HEIGHT + ROW_SPACING), faction.endLevel());
    drawLevelLabel(graphics, panelX, firstRowY + 3 * (ROW_HEIGHT + ROW_SPACING), faction.agiLevel());
    drawLevelLabel(graphics, panelX, firstRowY + 4 * (ROW_HEIGHT + ROW_SPACING), faction.staminaPoints());
    drawLevelLabel(graphics, panelX, firstRowY + 5 * (ROW_HEIGHT + ROW_SPACING), faction.oxygenPoints());

    if (UniqueSkillsHelper.hasAnyUniqueSkill(faction)) {
      drawUniqueSkillSection(graphics, panelX, panelY + statsSectionHeight(), faction);
    }

    for (GuiEventListener child : this.children()) {
      if (child instanceof AbstractWidget widget) {
        widget.render(graphics, mouseX, mouseY, partialTick);
      }
    }
  }

  private void drawUniqueSkillSection(GuiGraphics graphics, int panelX, int sectionY, PlayerFaction faction) {
    graphics.fill(panelX + 10, sectionY - 4, panelX + PANEL_WIDTH - 10, sectionY - 3, BORDER_COLOR);
    graphics.drawString(this.font, "COMPÉTENCE UNIQUE", panelX + 12, sectionY, UNIQUE_UNLOCKED_COLOR, false);
    graphics.drawString(this.font, uniqueSkillTitle(faction), panelX + 12, sectionY + 10, UNIQUE_UNLOCKED_COLOR, false);

    int descY = sectionY + 20;
    for (String line : uniqueSkillDescription(faction)) {
      graphics.drawString(this.font, line, panelX + 12, descY, DESC_COLOR, false);
      descY += LINE_HEIGHT;
    }
  }

  private static String uniqueSkillTitle(PlayerFaction faction) {
    if (faction.hasDualBlades()) {
      return "DOUBLES LAMES";
    }
    if (faction.hasBowSkill()) {
      return "ŒIL DE FAUCON";
    }
    if (faction.hasTridentSkill()) {
      return "MAÎTRE DU LAC";
    }
    if (faction.hasFrancTireurSkill()) {
      return "LE FRANC-TIREUR";
    }
    if (faction.hasShurikenSkill()) {
      return "SHURIKENJUTSU";
    }
    return "";
  }

  private static List<String> uniqueSkillDescription(PlayerFaction faction) {
    if (faction.hasDualBlades()) {
      return DUAL_BLADES_DESCRIPTION;
    }
    if (faction.hasBowSkill()) {
      return HAWKEYE_DESCRIPTION;
    }
    if (faction.hasTridentSkill()) {
      return LAKE_MASTER_DESCRIPTION;
    }
    if (faction.hasFrancTireurSkill()) {
      return FRANC_TIREUR_DESCRIPTION;
    }
    if (faction.hasShurikenSkill()) {
      return SHURIKEN_DESCRIPTION;
    }
    return List.of();
  }

  private void drawLevelLabel(GuiGraphics graphics, int panelX, int rowY, int level) {
    String text = "Nv. " + level;
    int textX = panelX + PANEL_WIDTH - 12 - this.font.width(text);
    graphics.drawString(this.font, text, textX, rowY + 6, LEVEL_COLOR, false);
  }

  private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
    graphics.fill(x, y, x + width, y + height, PANEL_BG_COLOR);
    graphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
    graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
    graphics.fill(x, y, x + 1, y + height, BORDER_COLOR);
    graphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);
  }

  @Override
  public void tick() {
    super.tick();
    int skillPoints = getCurrentFaction().skillPoints();
    if (skillPoints != cachedSkillPoints) {
      rebuildButtons();
    }
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  private final class SaoStatButton extends AbstractWidget {

    private final Runnable onPress;

    private SaoStatButton(int x, int y, int width, int height, Component message, boolean active, Runnable onPress) {
      super(x, y, width, height, message);
      this.active = active;
      this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      int fillColor = !this.active
          ? BUTTON_DISABLED_COLOR
          : this.isHovered() ? BUTTON_HOVER_COLOR : BUTTON_BG_COLOR;
      graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, fillColor);
      drawPanel(graphics, this.getX(), this.getY(), this.width, this.height);
      graphics.drawCenteredString(
          SkillMenuScreen.this.font,
          this.getMessage(),
          this.getX() + this.width / 2,
          this.getY() + (this.height - 8) / 2,
          TEXT_COLOR
      );
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
      if (this.active) {
        this.onPress.run();
      }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
    }
  }
}
