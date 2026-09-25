package com.seroka.client.screen;

import com.seroka.client.NavireFichePanneau;
import com.seroka.navire.NavireEntity;
import com.seroka.navire.NavireMenu;
import com.seroka.network.payload.DemonterNavirePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Écran de la soute : emplacements, fiche du navire, et retour en construction si la coque est intacte.
 *
 * <p>Une soute plus grande qu'un grand coffre se feuillette : flèches dans le bandeau du titre, ou
 * molette n'importe où dans l'écran.
 */
public class NavireScreen extends AbstractContainerScreen<NavireMenu> {

  private static final ResourceLocation TEXTURE =
      ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

  /** Hauteur de la texture au-dessus et en dessous des rangées de la soute. */
  private static final int BORDS = 114;

  /** Fond gris de la texture de coffre : masque les cases absentes d'une dernière page courte. */
  private static final int FOND_PANNEAU = 0xFFC6C6C6;

  private static final int TAILLE_FLECHE = 12;

  /** Place du numéro de page entre les deux flèches. */
  private static final int LARGEUR_NUMERO = 34;

  private final int rangees;
  @Nullable
  private Button precedente;
  @Nullable
  private Button suivante;

  public NavireScreen(NavireMenu menu, Inventory inventaire, Component titre) {
    super(menu, inventaire, titre);
    this.rangees = menu.rangees();
    this.imageHeight = BORDS + rangees * 18;
    this.inventoryLabelY = imageHeight - 94;
  }

  @Override
  protected void init() {
    super.init();
    NavireEntity navire = navire();
    boolean intact = navire != null && navire.estIntact();
    Button bouton = Button.builder(
            Component.translatable("navire.seroka.modifier"),
            ignored -> PacketDistributor.sendToServer(new DemonterNavirePayload(menu.entityId())))
        .bounds(leftPos, topPos + imageHeight + 4, imageWidth, 20)
        .tooltip(Tooltip.create(Component.translatable(
            intact ? "navire.seroka.modifier.hint" : "navire.seroka.modifier.interdit")))
        .build();
    bouton.active = intact;
    addRenderableWidget(bouton);

    if (menu.pages() > 1) {
      int y = topPos + 3;
      int xSuivante = leftPos + imageWidth - 7 - TAILLE_FLECHE;
      int xPrecedente = xSuivante - LARGEUR_NUMERO - TAILLE_FLECHE;
      precedente = addRenderableWidget(Button.builder(Component.literal("<"),
              ignored -> allerALaPage(menu.page() - 1))
          .bounds(xPrecedente, y, TAILLE_FLECHE, TAILLE_FLECHE)
          .tooltip(Tooltip.create(Component.translatable("navire.seroka.soute.precedente")))
          .build());
      suivante = addRenderableWidget(Button.builder(Component.literal(">"),
              ignored -> allerALaPage(menu.page() + 1))
          .bounds(xSuivante, y, TAILLE_FLECHE, TAILLE_FLECHE)
          .tooltip(Tooltip.create(Component.translatable("navire.seroka.soute.suivante")))
          .build());
      majFleches();
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    if (menu.pages() > 1 && scrollY != 0.0D) {
      allerALaPage(menu.page() + (scrollY > 0.0D ? -1 : 1));
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  private void allerALaPage(int page) {
    int avant = menu.page();
    menu.changerDePage(page);
    if (menu.page() == avant || minecraft == null || minecraft.gameMode == null) {
      return;
    }
    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.page());
    majFleches();
  }

  private void majFleches() {
    if (precedente != null) {
      precedente.active = menu.page() > 0;
    }
    if (suivante != null) {
      suivante.active = menu.page() < menu.pages() - 1;
    }
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    super.render(graphics, mouseX, mouseY, partialTick);
    renderTooltip(graphics, mouseX, mouseY);
    NavireEntity navire = navire();
    if (navire != null) {
      NavireFichePanneau.aCoteDe(graphics, this, navire);
    }
  }

  @Override
  protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    super.renderLabels(graphics, mouseX, mouseY);
    if (menu.pages() > 1) {
      Component numero = Component.translatable("navire.seroka.soute.page",
          menu.page() + 1, menu.pages());
      int centre = imageWidth - 7 - TAILLE_FLECHE - LARGEUR_NUMERO / 2;
      graphics.drawString(font, numero, centre - font.width(numero) / 2, titleLabelY, 0x404040,
          false);
    }
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, rangees * 18 + 17);
    graphics.blit(TEXTURE, leftPos, topPos + rangees * 18 + 17, 0, 126, imageWidth, 96);
    int garnies = menu.rangeesDeLaPage();
    if (garnies < rangees) {
      graphics.fill(leftPos + 7, topPos + 17 + garnies * 18,
          leftPos + 7 + NavireMenu.COLONNES * 18, topPos + 17 + rangees * 18, FOND_PANNEAU);
    }
  }

  @Nullable
  private NavireEntity navire() {
    if (minecraft == null || minecraft.level == null) {
      return null;
    }
    Entity entite = minecraft.level.getEntity(menu.entityId());
    return entite instanceof NavireEntity trouve ? trouve : null;
  }
}
