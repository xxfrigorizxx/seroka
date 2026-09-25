package com.seroka.chimere;

import com.seroka.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Flèches empoisonnées de l'Embourbé — confinées à l'inventaire du joueur. */
public final class FlecheEmpoisonneeHelper {

  public static final String POISON_ARROW_PROJECTILE_TAG = "seroka_poison_arrow";

  private FlecheEmpoisonneeHelper() {}

  public static boolean isPoisonArrow(ItemStack stack) {
    return !stack.isEmpty() && stack.is(ModRegistry.FLECHE_EMPOISONNEE.get());
  }

  public static ItemStack createStack(int count) {
    return new ItemStack(ModRegistry.FLECHE_EMPOISONNEE.get(), count);
  }

  public static void purgeOutsidePlayerInventory(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EMBOURBE)) {
      return;
    }

    AbstractContainerMenu menu = player.containerMenu;
    if (menu != player.inventoryMenu) {
      for (Slot slot : menu.slots) {
        if (slot.container == player.getInventory()) {
          continue;
        }
        if (isPoisonArrow(slot.getItem())) {
          slot.set(ItemStack.EMPTY);
        }
      }
    }
  }

  public static void purgeAllPoisonArrows(ServerPlayer player) {
    purgeFromInventory(player, player.getInventory());
    if (isPoisonArrow(player.containerMenu.getCarried())) {
      player.containerMenu.setCarried(ItemStack.EMPTY);
    }
  }

  private static void purgeFromInventory(Player player, Inventory inventory) {
    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
      ItemStack stack = inventory.getItem(slot);
      if (isPoisonArrow(stack)) {
        inventory.setItem(slot, ItemStack.EMPTY);
      }
    }
  }
}
