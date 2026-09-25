package com.seroka.chimere;

import com.seroka.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Flèches de lenteur du Vagabond — confinées à l'inventaire du joueur. */
public final class FlecheLenteurHelper {

  public static final String SLOW_ARROW_PROJECTILE_TAG = "seroka_slow_arrow";

  private FlecheLenteurHelper() {}

  public static boolean isSlowArrow(ItemStack stack) {
    return !stack.isEmpty() && stack.is(ModRegistry.FLECHE_LENTEUR.get());
  }

  public static ItemStack createStack(int count) {
    return new ItemStack(ModRegistry.FLECHE_LENTEUR.get(), count);
  }

  public static void purgeOutsidePlayerInventory(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VAGABOND)) {
      return;
    }

    AbstractContainerMenu menu = player.containerMenu;
    if (menu != player.inventoryMenu) {
      for (Slot slot : menu.slots) {
        if (slot.container == player.getInventory()) {
          continue;
        }
        if (isSlowArrow(slot.getItem())) {
          slot.set(ItemStack.EMPTY);
        }
      }
    }
  }

  public static void purgeAllSlowArrows(ServerPlayer player) {
    purgeFromInventory(player, player.getInventory());
    if (isSlowArrow(player.containerMenu.getCarried())) {
      player.containerMenu.setCarried(ItemStack.EMPTY);
    }
  }

  private static void purgeFromInventory(Player player, Inventory inventory) {
    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
      ItemStack stack = inventory.getItem(slot);
      if (isSlowArrow(stack)) {
        inventory.setItem(slot, ItemStack.EMPTY);
      }
    }
  }
}
