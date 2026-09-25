package com.seroka.chimere;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Stockage de selle du Dieu Cheval — 9 slots persistants. */
public record ChevalSaddlebag(List<ItemStack> slots) {

  public static final int SIZE = 9;
  public static final ChevalSaddlebag EMPTY = new ChevalSaddlebag(emptySlots());

  public static final Codec<ChevalSaddlebag> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      // OPTIONAL_CODEC et non CODEC : les emplacements libres du sac sont des piles vides,
      // que le codec strict rejette — la sacoche entière échouait alors à la sauvegarde.
      ItemStack.OPTIONAL_CODEC.listOf().fieldOf("slots").forGetter(ChevalSaddlebag::slots)
  ).apply(instance, ChevalSaddlebag::new));

  public ChevalSaddlebag {
    slots = List.copyOf(normalizedSlots(slots));
  }

  public static List<ItemStack> emptySlots() {
    NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    return List.copyOf(items);
  }

  private static List<ItemStack> normalizedSlots(List<ItemStack> source) {
    NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    if (source != null) {
      for (int i = 0; i < Math.min(SIZE, source.size()); i++) {
        ItemStack stack = source.get(i);
        items.set(i, stack == null ? ItemStack.EMPTY : stack.copy());
      }
    }
    return List.copyOf(items);
  }

  public ItemStack get(int index) {
    return slots.get(index);
  }

  public ChevalSaddlebag withSlot(int index, ItemStack stack) {
    List<ItemStack> updated = new ArrayList<>(slots);
    updated.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
    return new ChevalSaddlebag(updated);
  }

  public ChevalSaddlebag copy() {
    return new ChevalSaddlebag(slots);
  }
}
