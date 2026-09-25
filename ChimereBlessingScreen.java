package com.seroka.client.screen;

import com.seroka.chimere.ChimereBlessing;
import com.seroka.chimere.GodBlessingDescriptions;
import com.seroka.chimere.GodCatalog;
import com.seroka.chimere.GodDefinition;
import com.seroka.faction.ModAttachments;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Menu K Chimère : dieu bénisseur et effets de la bénédiction active. */
public class ChimereBlessingScreen extends Screen {

  private static final int OVERLAY_COLOR = 0xC0000000;
  private static final int PANEL_BG_COLOR = 0xC0282828;
  private static final int BORDER_COLOR = 0xFFFFFFFF;
  private static final int TEXT_COLOR = 0xFFFFFFFF;
  private static final int TITLE_COLOR = 0xFFFFD966;
  private static final int DESC_COLOR = 0xFFCCCCCC;
  private static final int MUTED_COLOR = 0xFF888888;

  private static final int PANEL_WIDTH = 280;
  private static final int PANEL_MARGIN = 16;
  private static final int PANEL_PADDING = 12;
  private static final int LINE_HEIGHT = 10;
  private static final int SECTION_GAP = 6;

  public ChimereBlessingScreen() {
    super(Component.translatable("screen.seroka.chimere_blessing"));
  }

  private ChimereBlessing currentBlessing() {
    if (this.minecraft == null || this.minecraft.player == null) {
      return ChimereBlessing.NONE;
    }
    return this.minecraft.player.getData(ModAttachments.CHIMERE_BLESSING);
  }

  private int contentWidth() {
    return PANEL_WIDTH - PANEL_PADDING * 2;
  }

  private List<FormattedCharSequence> wrapEffectLines(String godId) {
    List<FormattedCharSequence> wrapped = new ArrayList<>();
    for (Component effect : GodBlessingDescriptions.effectsFor(godId)) {
      wrapped.addAll(this.font.split(effect, contentWidth()));
    }
    return wrapped;
  }

  private int panelHeight(ChimereBlessing blessing) {
    if (!blessing.isActive()) {
      return PANEL_PADDING + LINE_HEIGHT + SECTION_GAP + LINE_HEIGHT + PANEL_PADDING;
    }

    int height = PANEL_PADDING;
    height += LINE_HEIGHT; // titre
    height += SECTION_GAP + LINE_HEIGHT; // dieu
    height += SECTION_GAP + LINE_HEIGHT; // activation
    height += SECTION_GAP + 1 + SECTION_GAP; // séparateur
    height += LINE_HEIGHT; // en-tête effets
    height += SECTION_GAP;
    height += wrapEffectLines(blessing.godId()).size() * LINE_HEIGHT;
    height += PANEL_PADDING;
    return Math.min(height, this.height - PANEL_MARGIN * 2);
  }

  private int panelX() {
    return (this.width - PANEL_WIDTH) / 2;
  }

  private int panelY(int panelHeight) {
    return Math.max(PANEL_MARGIN, (this.height - panelHeight) / 2);
  }

  @Override
  public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    graphics.fill(0, 0, this.width, this.height, OVERLAY_COLOR);
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    this.renderBackground(graphics, mouseX, mouseY, partialTick);

    ChimereBlessing blessing = currentBlessing();
    int panelHeight = panelHeight(blessing);
    int panelX = panelX();
    int panelY = panelY(panelHeight);
    int textX = panelX + PANEL_PADDING;

    drawPanel(graphics, panelX, panelY, PANEL_WIDTH, panelHeight);

    int y = panelY + PANEL_PADDING;
    graphics.drawString(
        this.font,
        Component.translatable("blessing.seroka.menu.title"),
        textX,
        y,
        TEXT_COLOR,
        false
    );

    if (!blessing.isActive()) {
      y += LINE_HEIGHT + SECTION_GAP;
      graphics.drawString(
          this.font,
          Component.translatable("blessing.seroka.menu.none"),
          textX,
          y,
          MUTED_COLOR,
          false
      );
      return;
    }

    GodDefinition god = GodCatalog.findById(blessing.godId());
    Component godName = god != null
        ? Component.translatable(god.translationKey() + ".display")
        : Component.literal(blessing.godId());

    y += LINE_HEIGHT + SECTION_GAP;
    graphics.drawString(this.font, godName, textX, y, TITLE_COLOR, false);

    y += LINE_HEIGHT + SECTION_GAP;
    graphics.drawString(
        this.font,
        GodBlessingDescriptions.activationFor(blessing.godId()),
        textX,
        y,
        DESC_COLOR,
        false
    );

    y += LINE_HEIGHT + SECTION_GAP;
    graphics.fill(textX, y, panelX + PANEL_WIDTH - PANEL_PADDING, y + 1, BORDER_COLOR);

    y += SECTION_GAP + 1;
    graphics.drawString(
        this.font,
        Component.translatable("blessing.seroka.menu.effects"),
        textX,
        y,
        TEXT_COLOR,
        false
    );

    y += LINE_HEIGHT + SECTION_GAP;
    for (FormattedCharSequence line : wrapEffectLines(blessing.godId())) {
      graphics.drawString(this.font, line, textX, y, DESC_COLOR, false);
      y += LINE_HEIGHT;
    }
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
