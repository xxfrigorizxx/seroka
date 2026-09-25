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

/** Menu 3x3 du sac du Dieu Lama. */
public class LamaPackMenu extends AbstractContainerMenu {

  private static final int PACK_START_X = 62;
  private static final int PACK_START_Y = 17;

  private final SimpleContainer packContainer;
  private final ServerPlayer owner;

  public LamaPackMenu(int containerId, Inventory playerInventory) {
    this(containerId, playerInventory, new SimpleContainer(LamaPack.SIZE), null);
  }

  public LamaPackMenu(int containerId, Inventory playerInventory, ServerPlayer owner) {
    this(containerId, playerInventory, loadContainer(owner), owner);
  }

  private LamaPackMenu(
      int containerId,
      Inventory playerInventory,
      SimpleContainer packContainer,
      ServerPlayer owner
  ) {
    super(ModRegistry.LAMA_PACK_MENU.get(), containerId);
    this.packContainer = packContainer;
    this.owner = owner;
    checkContainerSize(packContainer, LamaPack.SIZE);

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        addSlot(new Slot(packContainer, col + row * 3, PACK_START_X + col * 18, PACK_START_Y + row * 18));
      }
    }

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
      }
    }
    for (int col = 0; col < 9; col++) {
      addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }
  }

  private static SimpleContainer loadContainer(ServerPlayer owner) {
    SimpleContainer container = new SimpleContainer(LamaPack.SIZE);
    LamaPack pack = LamaBlessingHandler.getPack(owner);
    for (int i = 0; i < LamaPack.SIZE; i++) {
      container.setItem(i, pack.get(i).copy());
    }
    return container;
  }

  @Override
  public boolean stillValid(Player player) {
    return ChimereBlessingService.hasBlessing(player, GodIds.LAMA);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    ItemStack result = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot.hasItem()) {
      ItemStack stack = slot.getItem();
      result = stack.copy();
      if (index < LamaPack.SIZE) {
        if (!this.moveItemStackTo(stack, LamaPack.SIZE, this.slots.size(), true)) {
          return ItemStack.EMPTY;
        }
      } else if (!this.moveItemStackTo(stack, 0, LamaPack.SIZE, false)) {
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
    LamaPack saved = new LamaPack(new ArrayList<>(packContainer.getItems()));
    LamaBlessingHandler.savePack(owner, saved);
  }
}
