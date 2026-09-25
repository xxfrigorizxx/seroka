package com.seroka.api;

import com.seroka.faction.FactionIds;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import net.minecraft.world.entity.player.Player;

/**
 * API publique Seroka pour les addons optionnels ({@code seroka_compat}, {@code seroka-<mod>}, …).
 * <p>
 * Seroka = mod de base (factions, règles, contenu). Les addons <b>complètent</b> Seroka en
 * branchant des mods externes ; ils ne sont pas obligatoires pour jouer.
 * <p>
 * {@link FactionIds#FRONTALIER} est une faction parmi d'autres ; d'autres IDs suivront.
 */
public final class SerokaAPI {

  public static final String MOD_ID = "seroka";

  private SerokaAPI() {}

  public static PlayerFaction getFaction(Player player) {
    return player.getData(ModAttachments.PLAYER_FACTION);
  }

  public static boolean isFrontalier(Player player) {
    return getFaction(player).isFrontalier();
  }

  public static boolean isChimere(Player player) {
    return getFaction(player).isChimere();
  }

  public static boolean isTetrasomie(Player player) {
    return getFaction(player).isTetrasomie();
  }

  public static boolean hasFaction(Player player, String factionId) {
    return factionId.equals(getFaction(player).faction());
  }

  public static String noneFactionId() {
    return FactionIds.NONE;
  }

  public static String frontalierFactionId() {
    return FactionIds.FRONTALIER;
  }

  public static String chimereFactionId() {
    return FactionIds.CHIMERE;
  }

  public static String tetrasomieFactionId() {
    return FactionIds.TETRASOMIE;
  }

  public static String getTetrasomieElement(Player player) {
    return getFaction(player).getElement();
  }
}
