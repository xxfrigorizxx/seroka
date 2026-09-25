package com.seroka.chimere;

/** Mode d'activation affiché dans le menu K. */
public enum GodActivation {
  MIDDLE_CLICK("blessing.seroka.menu.activation"),
  PASSIVE("blessing.seroka.menu.activation.passive"),
  PASSIVE_AND_F("blessing.seroka.menu.activation.passive_and_f"),
  PASSIVE_AND_MIDDLE("blessing.seroka.menu.activation.passive_and_middle"),
  PASSIVE_MIDDLE_AND_F("blessing.seroka.menu.activation.passive_middle_and_f"),
  PASSIVE_MIDDLE_F_AND_H("blessing.seroka.menu.activation.passive_middle_f_and_h"),
  MIDDLE_AND_F("blessing.seroka.menu.activation.middle_and_f");

  private final String translationKey;

  GodActivation(String translationKey) {
    this.translationKey = translationKey;
  }

  public String translationKey() {
    return translationKey;
  }
}
