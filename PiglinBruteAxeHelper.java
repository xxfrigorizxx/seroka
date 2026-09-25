package com.seroka.chimere;

import com.seroka.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Hache du Piglin Brute — confinée à l'inventaire pendant la compétence. */
public final class PiglinBruteAxeHelper {

  private PiglinBruteAxeHelper() {}

  public static boolean isBruteAxe(ItemStack stack) {
    return !stack.isEmpty() && stack.is(ModRegistry.HACHE_PIGLIN_BRUTE.get());
  }

  public static ItemStack createStack() {
    return new ItemStack(ModRegistry.HACHE_PIGLIN_BRUTE.get());
  }

  public static void purgeOutsidePlayerInventory(ServerPlayer player) {
    if (!PiglinBruteBlessingHandler.isBruteAxeAuthorized(player)) {
      return;
    }

    AbstractContainerMenu menu = player.containerMenu;
    if (menu != player.inventoryMenu) {
      for (Slot slot : menu.slots) {
        if (slot.container == player.getInventory()) {
          continue;
        }
        if (isBruteAxe(slot.getItem())) {
          slot.set(ItemStack.EMPTY);
        }
      }
    }
  }

  public static void purgeAllBruteAxes(ServerPlayer player) {
    purgeFromInventory(player, player.getInventory());
    if (isBruteAxe(player.containerMenu.getCarried())) {
      player.containerMenu.setCarried(ItemStack.EMPTY);
    }
    purgeOutsidePlayerInventory(player);
  }

  public static boolean hasBruteAxeInInventory(Player player) {
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      if (isBruteAxe(player.getInventory().getItem(slot))) {
        return true;
      }
    }
    return isBruteAxe(player.getMainHandItem()) || isBruteAxe(player.getOffhandItem());
  }

  public static void vanish(ServerPlayer player, ItemStack stack) {
    if (!isBruteAxe(stack)) {
      return;
    }
    stack.setCount(0);
    player.level().addParticle(
        ParticleTypes.SMOKE,
        player.getX(),
        player.getY() + 1.0D,
        player.getZ(),
        0.0D,
        0.05D,
        0.0D
    );
    player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 1.2F);
  }

  public static void discardItemEntity(ItemEntity itemEntity) {
    if (isBruteAxe(itemEntity.getItem())) {
      itemEntity.discard();
    }
  }

  private static void purgeFromInventory(Player player, Inventory inventory) {
    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
      ItemStack stack = inventory.getItem(slot);
      if (isBruteAxe(stack)) {
        inventory.setItem(slot, ItemStack.EMPTY);
      }
    }
  }
}
