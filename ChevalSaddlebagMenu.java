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

/** Menu 3x3 de la selle du Dieu Cheval. */
public class ChevalSaddlebagMenu extends AbstractContainerMenu {

  private static final int SADDLE_START_X = 62;
  private static final int SADDLE_START_Y = 17;

  private final SimpleContainer saddlebag;
  private final ServerPlayer owner;

  public ChevalSaddlebagMenu(int containerId, Inventory playerInventory) {
    this(containerId, playerInventory, new SimpleContainer(ChevalSaddlebag.SIZE), null);
  }

  public ChevalSaddlebagMenu(int containerId, Inventory playerInventory, ServerPlayer owner) {
    this(containerId, playerInventory, loadContainer(owner), owner);
  }

  private ChevalSaddlebagMenu(
      int containerId,
      Inventory playerInventory,
      SimpleContainer saddlebag,
      ServerPlayer owner
  ) {
    super(ModRegistry.CHEVAL_SADDLEBAG_MENU.get(), containerId);
    this.saddlebag = saddlebag;
    this.owner = owner;
    checkContainerSize(saddlebag, ChevalSaddlebag.SIZE);

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        addSlot(new Slot(saddlebag, col + row * 3, SADDLE_START_X + col * 18, SADDLE_START_Y + row * 18));
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
    SimpleContainer container = new SimpleContainer(ChevalSaddlebag.SIZE);
    ChevalSaddlebag saddlebag = ChevalBlessingHandler.getSaddlebag(owner);
    for (int i = 0; i < ChevalSaddlebag.SIZE; i++) {
      container.setItem(i, saddlebag.get(i).copy());
    }
    return container;
  }

  @Override
  public boolean stillValid(Player player) {
    return ChimereBlessingService.hasBlessing(player, GodIds.CHEVAL);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    ItemStack result = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot.hasItem()) {
      ItemStack stack = slot.getItem();
      result = stack.copy();
      if (index < ChevalSaddlebag.SIZE) {
        if (!this.moveItemStackTo(stack, ChevalSaddlebag.SIZE, this.slots.size(), true)) {
          return ItemStack.EMPTY;
        }
      } else if (!this.moveItemStackTo(stack, 0, ChevalSaddlebag.SIZE, false)) {
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
    ChevalSaddlebag saved = new ChevalSaddlebag(new ArrayList<>(saddlebag.getItems()));
    ChevalBlessingHandler.saveSaddlebag(owner, saved);
  }
}
