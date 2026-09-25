package com.seroka.chimere;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.ItemStack;

/** Bénédiction du Dieu Marchand Ambulant : échanges villageois par quantité seulement. */
public final class MarchandAmbulantBlessingHandler {

  private MarchandAmbulantBlessingHandler() {}

  public static boolean isActive(Player player) {
    return ChimereBlessingService.hasBlessing(player, GodIds.MARCHAND_AMBULANT);
  }

  public static boolean isVillagerTrade(Merchant trader) {
    return trader instanceof Villager;
  }

  public static boolean isWanderingTraderTitle(Component title) {
    return Component.translatable("entity.minecraft.wandering_trader").equals(title);
  }

  public static boolean satisfiedByCount(MerchantOffer offer, ItemStack playerOfferA, ItemStack playerOfferB) {
    ItemStack costA = offer.getCostA();
    if (!costA.isEmpty()) {
      if (playerOfferA.isEmpty() || playerOfferA.getCount() < costA.getCount()) {
        return false;
      }
    } else if (!playerOfferA.isEmpty()) {
      return false;
    }

    ItemStack costB = offer.getCostB();
    if (!costB.isEmpty()) {
      if (playerOfferB.isEmpty() || playerOfferB.getCount() < costB.getCount()) {
        return false;
      }
    } else if (!playerOfferB.isEmpty()) {
      return false;
    }
    return true;
  }

  public static boolean takeCount(MerchantOffer offer, ItemStack playerOfferA, ItemStack playerOfferB) {
    if (!satisfiedByCount(offer, playerOfferA, playerOfferB)) {
      return false;
    }
    ItemStack costA = offer.getCostA();
    if (!costA.isEmpty()) {
      playerOfferA.shrink(costA.getCount());
    }
    ItemStack costB = offer.getCostB();
    if (!costB.isEmpty()) {
      playerOfferB.shrink(costB.getCount());
    }
    return true;
  }
}
