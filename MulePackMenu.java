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

/** Menu 3x4 du sac de bât + inventaire joueur. */
public class MulePackMenu extends AbstractContainerMenu {

  public static final int PACK_ROWS = 4;
  public static final int PACK_COLS = 3;
  public static final int PACK_START_X = 62;
  public static final int PACK_SLOT_WIDTH = PACK_COLS * 18;
  private static final int PACK_START_Y = 17;

  private final SimpleContainer pack;
  private final ServerPlayer owner;

  public MulePackMenu(int containerId, Inventory playerInventory) {
    this(containerId, playerInventory, new SimpleContainer(MulePack.SIZE), null);
  }

  public MulePackMenu(int containerId, Inventory playerInventory, ServerPlayer owner) {
    this(containerId, playerInventory, loadContainer(owner), owner);
  }

  private MulePackMenu(int containerId, Inventory playerInventory, SimpleContainer pack, ServerPlayer owner) {
    super(ModRegistry.MULE_PACK_MENU.get(), containerId);
    this.pack = pack;
    this.owner = owner;
    checkContainerSize(pack, MulePack.SIZE);

    for (int row = 0; row < PACK_ROWS; row++) {
      for (int col = 0; col < PACK_COLS; col++) {
        addSlot(new Slot(pack, col + row * PACK_COLS, PACK_START_X + col * 18, PACK_START_Y + row * 18));
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
    SimpleContainer container = new SimpleContainer(MulePack.SIZE);
    MulePack pack = MuleBlessingHandler.getPack(owner);
    for (int i = 0; i < MulePack.SIZE; i++) {
      container.setItem(i, pack.get(i).copy());
    }
    return container;
  }

  @Override
  public boolean stillValid(Player player) {
    return ChimereBlessingService.hasBlessing(player, GodIds.MULE);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    ItemStack result = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot.hasItem()) {
      ItemStack stack = slot.getItem();
      result = stack.copy();
      if (index < MulePack.SIZE) {
        if (!this.moveItemStackTo(stack, MulePack.SIZE, this.slots.size(), true)) {
          return ItemStack.EMPTY;
        }
      } else if (!this.moveItemStackTo(stack, 0, MulePack.SIZE, false)) {
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
    MulePack saved = new MulePack(new ArrayList<>(pack.getItems()));
    MuleBlessingHandler.savePack(owner, saved);
  }
}
