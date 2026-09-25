package com.seroka.client;

import com.seroka.navire.CanonLogique;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * Recul des canons, côté client seulement.
 *
 * <p>On ne retient que l'instant du coup : le va-et-vient s'en déduit image par image, ce qui donne
 * un mouvement fluide sans rien diffuser pendant l'animation.
 */
public final class CanonReculClient {

  /** Un canon repéré soit sur une coque, soit au sol ({@code navireId} négatif). */
  private record Piece(int navireId, BlockPos culasse) {}

  /** Part de l'animation passée à partir en arrière ; le reste sert à revenir en batterie. */
  private static final float PART_DEPART = 0.22F;
  /** Course de l'affût sur ses roues, en blocs. */
  private static final float COURSE = 0.55F;

  private static final Map<Piece, Long> DERNIERS_TIRS = new HashMap<>();

  private CanonReculClient() {}

  public static void noterTir(int navireId, BlockPos culasse) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return;
    }
    DERNIERS_TIRS.put(new Piece(navireId, culasse), minecraft.level.getGameTime());
  }

  /**
   * Recul du canon à cet instant, en blocs vers l'arrière.
   *
   * <p>Le départ est sec et le retour lent : c'est ce qui se lit comme un canon qui encaisse son
   * coup puis se laisse ramener en batterie.
   */
  public static float recul(int navireId, BlockPos culasse, float partialTick) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return 0.0F;
    }
    Piece piece = new Piece(navireId, culasse);
    Long tir = DERNIERS_TIRS.get(piece);
    if (tir == null) {
      return 0.0F;
    }
    float ecoule = minecraft.level.getGameTime() - tir + partialTick;
    if (ecoule < 0.0F || ecoule > CanonLogique.TICKS_RECUL) {
      DERNIERS_TIRS.remove(piece);
      return 0.0F;
    }
    float avance = ecoule / CanonLogique.TICKS_RECUL;
    float courbe = avance < PART_DEPART
        ? avance / PART_DEPART
        : 1.0F - (avance - PART_DEPART) / (1.0F - PART_DEPART);
    return COURSE * Mth.clamp(courbe, 0.0F, 1.0F);
  }
}
