package com.seroka.chimere;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Catalogue des dieux — ajoutés un par un au fil des tests.
 * Tant que {@link #ALL} est vide, aucune bénédiction ne peut être tirée.
 */
public final class GodCatalog {

  private static final List<GodDefinition> GODS = new ArrayList<>();

  public static final List<GodDefinition> ALL = Collections.unmodifiableList(GODS);

  private GodCatalog() {}

  /** Enregistre ou met à jour un dieu (remplace l'ancienne définition si l'id existe déjà). */
  public static void register(GodDefinition god) {
    GodDefinition existing = findById(god.id());
    if (existing != null) {
      GODS.remove(existing);
    }
    GODS.add(god);
  }

  public static GodDefinition findById(String godId) {
    for (GodDefinition god : GODS) {
      if (god.id().equals(godId)) {
        return god;
      }
    }
    return null;
  }
}
