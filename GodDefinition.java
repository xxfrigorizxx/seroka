package com.seroka.chimere;

/**
 * Définition d'un dieu : quota global serveur et clé de traduction.
 *
 * @param id          identifiant stable (voir {@link GodIds})
 * @param maxBlessings nombre max de joueurs bénis simultanément sur le serveur
 * @param translationKey clé {@code blessing.seroka.god.<id>}
 */
public record GodDefinition(String id, int maxBlessings, String translationKey) {

  public boolean hasCapacity(int activeBlessings) {
    return activeBlessings < maxBlessings;
  }
}
