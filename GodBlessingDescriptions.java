package com.seroka.chimere;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Descriptions d'effets affichées dans le menu Chimère (K). */
public final class GodBlessingDescriptions {

  private static final Map<String, List<String>> EFFECT_KEYS = new HashMap<>();
  private static final Map<String, GodActivation> ACTIVATION_BY_GOD = new HashMap<>();

  private GodBlessingDescriptions() {}

  public static void register(String godId, List<String> effectTranslationKeys) {
    register(godId, GodActivation.MIDDLE_CLICK, effectTranslationKeys);
  }

  public static void register(String godId, GodActivation activation, List<String> effectTranslationKeys) {
    EFFECT_KEYS.put(godId, List.copyOf(effectTranslationKeys));
    ACTIVATION_BY_GOD.put(godId, activation);
  }

  public static Component activationFor(String godId) {
    GodActivation activation = ACTIVATION_BY_GOD.getOrDefault(godId, GodActivation.MIDDLE_CLICK);
    return Component.translatable(activation.translationKey());
  }

  public static List<Component> effectsFor(String godId) {
    List<String> keys = EFFECT_KEYS.get(godId);
    if (keys == null || keys.isEmpty()) {
      return List.of();
    }
    List<Component> lines = new ArrayList<>(keys.size());
    for (String key : keys) {
      lines.add(Component.translatable(key));
    }
    return Collections.unmodifiableList(lines);
  }
}
