package com.seroka.client;

import com.seroka.navire.NavireEntity;
import com.seroka.navire.VoileNavire;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Réception des consignes de voile côté client.
 *
 * <p>Le client garde déjà la coque : il retrouve donc la voile par son ancre au lieu de tenir une
 * carte parallèle, et n'a plus qu'à recopier le réglage.
 */
public final class VoileClient {

  private VoileClient() {}

  public static void recevoir(int navireId, BlockPos ancre, float deploiement, float cible) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return;
    }
    if (!(minecraft.level.getEntity(navireId) instanceof NavireEntity navire)) {
      return;
    }
    VoileNavire voile = navire.voiles().get(ancre);
    if (voile == null) {
      return;
    }
    voile.definirDeploiement(deploiement);
    voile.viser(cible);
  }
}
