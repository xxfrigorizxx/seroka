package com.seroka.chimere;

import com.seroka.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

/** Menu 9x2 du sac de bât + inventaire joueur. */
public class AnePackMenu extends AbstractContainerMenu {

  public static final int PACK_ROWS = 2;
  private static final int PACK_COLS = 9;

  private final SimpleContainer pack;
  private final ServerPlayer owner;

  public AnePackMenu(int containerId, Inventory playerInventory) {
    this(containerId, playerInventory, new SimpleContainer(AnePack.SIZE), null);
  }

  public AnePackMenu(int containerId, Inventory playerInventory, ServerPlayer owner) {
    this(containerId, playerInventory, loadContainer(owner), owner);
  }

  private AnePackMenu(int containerId, Inventory playerInventory, SimpleContainer pack, ServerPlayer owner) {
    super(ModRegistry.ANE_PACK_MENU.get(), containerId);
    this.pack = pack;
    this.owner = owner;
    checkContainerSize(pack, AnePack.SIZE);

    for (int row = 0; row < PACK_ROWS; row++) {
      for (int col = 0; col < PACK_COLS; col++) {
        addSlot(new Slot(pack, col + row * PACK_COLS, 8 + col * 18, 18 + row * 18));
      }
    }

    int playerInvY = PACK_ROWS * 18 + 30;
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
      }
    }
    int hotbarY = PACK_ROWS * 18 + 88;
    for (int col = 0; col < 9; col++) {
      addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
    }
  }

  private static SimpleContainer loadContainer(ServerPlayer owner) {
    SimpleContainer container = new SimpleContainer(AnePack.SIZE);
    AnePack pack = AneBlessingHandler.getPack(owner);
    for (int i = 0; i < AnePack.SIZE; i++) {
      container.setItem(i, pack.get(i).copy());
    }
    return container;
  }

  @Override
  public boolean stillValid(Player player) {
    return ChimereBlessingService.hasBlessing(player, GodIds.ANE);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    ItemStack result = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot.hasItem()) {
      ItemStack stack = slot.getItem();
      result = stack.copy();
      if (index < AnePack.SIZE) {
        if (!this.moveItemStackTo(stack, AnePack.SIZE, this.slots.size(), true)) {
          return ItemStack.EMPTY;
        }
      } else if (!this.moveItemStackTo(stack, 0, AnePack.SIZE, false)) {
        return ItemStack.EMPTY;
      }

      if (stack.isEmpty()) {
        slot.setByPlayer(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }
    return result;
  }

  @Override
  public void removed(Player player) {
    super.removed(player);
    if (player.level().isClientSide() || owner == null) {
      return;
    }
    AnePack saved = new AnePack(new ArrayList<>(pack.getItems()));
    AneBlessingHandler.savePack(owner, saved);
  }
}
