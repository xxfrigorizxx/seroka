package com.seroka.chimere;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.SuspiciousStewEffects;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Cochon : mange tout sans effets négatifs. */
public final class CochonBlessingHandler {

  private static final Set<UUID> CONSUMING = ConcurrentHashMap.newKeySet();

  private CochonBlessingHandler() {}

  public static boolean isConsumable(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }
    if (stack.has(DataComponents.FOOD)) {
      return true;
    }
    PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
    if (potion != null && (potion.potion().isPresent() || !potion.customEffects().isEmpty())) {
      return true;
    }
    SuspiciousStewEffects stew = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
    return stew != null && !stew.effects().isEmpty();
  }

  public static void beginConsuming(ServerPlayer player) {
    CONSUMING.add(player.getUUID());
  }

  public static void endConsuming(ServerPlayer player) {
    CONSUMING.remove(player.getUUID());
  }

  public static boolean isConsuming(Player player) {
    return CONSUMING.contains(player.getUUID());
  }

  public static boolean shouldBlockNegativeEffect(Player player, MobEffectInstance effect) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.COCHON)) {
      return false;
    }
    if (!isConsuming(player)) {
      return false;
    }
    return !effect.getEffect().value().isBeneficial();
  }
}
