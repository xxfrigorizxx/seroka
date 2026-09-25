package com.seroka.client;

import com.seroka.ModMain;
import com.seroka.navire.FicheNavire;
import com.seroka.navire.NavireEntity;
import com.seroka.network.payload.OpenNavireMenuPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Fiche du navire affichée à côté de l'inventaire, quand le joueur se trouve à bord.
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class NavireFichePanneau {

  private static final int MARGE = 8;
  private static final int PADDING = 6;
  private static final int HAUTEUR_LIGNE = 11;
  private static final int FOND = 0xC0101014;
  private static final int BORDURE = 0xFF3A424C;

  private NavireFichePanneau() {}

  /**
   * À la barre, E ouvre la soute du navire au lieu de l'inventaire.
   *
   * <p>Priorité haute : un Frontalier à la barre doit voir la soute, pas l'écran de faction.
   */
  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void surOuvertureInventaire(ScreenEvent.Opening event) {
    if (!(event.getNewScreen() instanceof InventoryScreen)
        && !(event.getNewScreen() instanceof CreativeModeInventoryScreen)) {
      return;
    }
    Player joueur = Minecraft.getInstance().player;
    if (joueur == null || !(joueur.getVehicle() instanceof NavireEntity)) {
      return;
    }
    event.setCanceled(true);
    PacketDistributor.sendToServer(new OpenNavireMenuPayload());
  }

  @SubscribeEvent
  public static void surRenduEcran(ScreenEvent.Render.Post event) {
    if (!(event.getScreen() instanceof InventoryScreen ecran)) {
      return;
    }
    Player joueur = Minecraft.getInstance().player;
    if (joueur == null) {
      return;
    }
    NavireEntity navire = NavireEntity.porteurDe(joueur);
    if (navire == null) {
      return;
    }
    aCoteDe(event.getGuiGraphics(), ecran, navire);
  }

  /** Fiche courte à droite (ou à gauche) d'un écran de conteneur. */
  public static void aCoteDe(GuiGraphics graphics, AbstractContainerScreen<?> ecran, NavireEntity navire) {
    FicheNavire fiche = navire.fiche();
    if (fiche == null) {
      return;
    }
    dessiner(graphics, ecran, fiche.resume(navire.pointsVie(), navire.flottabilite()));
  }

  private static void dessiner(GuiGraphics graphics, AbstractContainerScreen<?> ecran,
      List<Component> lignes) {
    var police = Minecraft.getInstance().font;
    int largeurTexte = lignes.stream().mapToInt(police::width).max().orElse(0);
    int largeur = largeurTexte + PADDING * 2;
    int hauteur = lignes.size() * HAUTEUR_LIGNE + PADDING * 2;
    int x = ecran.getGuiLeft() + ecran.getXSize() + MARGE;
    int y = ecran.getGuiTop();

    // Replier le panneau à gauche de l'inventaire s'il déborde de l'écran.
    if (x + largeur > ecran.width) {
      x = ecran.getGuiLeft() - largeur - MARGE;
    }

    graphics.fill(x, y, x + largeur, y + hauteur, FOND);
    graphics.renderOutline(x, y, largeur, hauteur, BORDURE);
    for (int i = 0; i < lignes.size(); i++) {
      graphics.drawString(police, lignes.get(i), x + PADDING, y + PADDING + i * HAUTEUR_LIGNE, -1);
    }
  }
}
