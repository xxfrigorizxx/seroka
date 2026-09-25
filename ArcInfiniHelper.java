package com.seroka.chimere;

import com.seroka.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Arc infini du Dieu Squelette — un arc et une flèche rechargée en continu.
 */
public final class ArcInfiniHelper {

  private ArcInfiniHelper() {}

  public static boolean isArcInfini(ItemStack stack) {
    return !stack.isEmpty() && stack.is(ModRegistry.ARC_INFINIE.get());
  }

  public static ItemStack createArcStack() {
    return new ItemStack(ModRegistry.ARC_INFINIE.get());
  }

  public static int countArcItems(Player player) {
    int count = 0;
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      if (isArcInfini(player.getInventory().getItem(slot))) {
        count++;
      }
    }
    ItemStack carried = player.containerMenu.getCarried();
    if (isArcInfini(carried)) {
      count++;
    }
    return count;
  }

  public static void ensureIncarnationKit(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SQUELETTE)) {
      return;
    }
    if (countArcItems(player) == 0) {
      giveArcToPlayer(player);
    }
    ensureSingleArrow(player);
  }

  public static void ensureSingleArrow(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SQUELETTE)) {
      return;
    }
    int arrowCount = countArrows(player);
    if (arrowCount == 0) {
      if (!player.getInventory().add(new ItemStack(Items.ARROW))) {
        player.drop(new ItemStack(Items.ARROW), false);
      }
      return;
    }
    if (arrowCount > 1) {
      removeExcessArrows(player, arrowCount - 1);
    }
  }

  public static void clearKit(Player player) {
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (isArcInfini(stack)) {
        player.getInventory().setItem(slot, ItemStack.EMPTY);
      }
    }
    ItemStack carried = player.containerMenu.getCarried();
    if (isArcInfini(carried)) {
      player.containerMenu.setCarried(ItemStack.EMPTY);
    }
  }

  public static void vanishAsAcid(ServerPlayer player, ItemStack stack) {
    if (stack.isEmpty()) {
      return;
    }
    player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.2F);
    player.serverLevel().sendParticles(
        ParticleTypes.SMOKE,
        player.getX(),
        player.getY() + 1.0D,
        player.getZ(),
        12,
        0.2D,
        0.3D,
        0.2D,
        0.01D
    );
    stack.shrink(stack.getCount());
  }

  private static void giveArcToPlayer(ServerPlayer player) {
    ItemStack arc = createArcStack();
    if (!player.getInventory().add(arc)) {
      player.drop(arc, false);
    }
  }

  private static int countArrows(Player player) {
    int count = 0;
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (stack.is(Items.ARROW)) {
        count += stack.getCount();
      }
    }
    ItemStack carried = player.containerMenu.getCarried();
    if (carried.is(Items.ARROW)) {
      count += carried.getCount();
    }
    return count;
  }

  private static void removeExcessArrows(ServerPlayer player, int toRemove) {
    int remaining = toRemove;
    for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (!stack.is(Items.ARROW)) {
        continue;
      }
      int removed = Math.min(remaining, stack.getCount());
      stack.shrink(removed);
      if (stack.isEmpty()) {
        player.getInventory().setItem(slot, ItemStack.EMPTY);
      }
      remaining -= removed;
    }
    if (remaining > 0) {
      ItemStack carried = player.containerMenu.getCarried();
      if (carried.is(Items.ARROW)) {
        int removed = Math.min(remaining, carried.getCount());
        carried.shrink(removed);
        if (carried.isEmpty()) {
          player.containerMenu.setCarried(ItemStack.EMPTY);
        }
      }
    }
  }
}
