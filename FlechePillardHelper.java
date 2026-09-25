package com.seroka.chimere;

import com.seroka.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Flèches du Pillard — confinées à l'inventaire du joueur. */
public final class FlechePillardHelper {

  private FlechePillardHelper() {}

  public static boolean isPillardArrow(ItemStack stack) {
    return !stack.isEmpty() && stack.is(ModRegistry.FLECHE_PILLARD.get());
  }

  public static ItemStack createStack(int count) {
    return new ItemStack(ModRegistry.FLECHE_PILLARD.get(), count);
  }

  public static void purgeOutsidePlayerInventory(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PILLARD)) {
      return;
    }

    AbstractContainerMenu menu = player.containerMenu;
    if (menu != player.inventoryMenu) {
      for (Slot slot : menu.slots) {
        if (slot.container == player.getInventory()) {
          continue;
        }
        if (isPillardArrow(slot.getItem())) {
          slot.set(ItemStack.EMPTY);
        }
      }
    }
  }

  public static void purgeAllPillardArrows(ServerPlayer player) {
    purgeFromInventory(player, player.getInventory());
    if (isPillardArrow(player.containerMenu.getCarried())) {
      player.containerMenu.setCarried(ItemStack.EMPTY);
    }
  }

  private static void purgeFromInventory(Player player, Inventory inventory) {
    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
      ItemStack stack = inventory.getItem(slot);
      if (isPillardArrow(stack)) {
        inventory.setItem(slot, ItemStack.EMPTY);
      }
    }
  }
}
