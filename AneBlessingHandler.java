package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

/** Bénédiction du Dieu Âne : vitesse + step passifs, sac 18 slots au clic molette. */
public final class AneBlessingHandler {

  private static final ResourceLocation SPEED_ID = ModMain.id("ane_speed");
  private static final ResourceLocation STEP_ID = ModMain.id("ane_step");
  private static final double SPEED_BONUS = 0.15D;
  private static final double STEP_BONUS = 0.4D;

  private AneBlessingHandler() {}

  public static void applyPassive(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ANE)) {
      return;
    }
    setMultiplierModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, SPEED_BONUS);
    setValueModifier(player, Attributes.STEP_HEIGHT, STEP_ID, STEP_BONUS);
  }

  public static void clearPassive(Player player) {
    removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
    removeModifier(player, Attributes.STEP_HEIGHT, STEP_ID);
  }

  public static void activate(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ANE)) {
      return;
    }
    player.playNotifySound(SoundEvents.DONKEY_CHEST, SoundSource.PLAYERS, 0.7F, 1.0F);
    player.openMenu(createMenuProvider(player));
  }

  public static void clear(Player player) {
    clearPassive(player);
    if (!(player instanceof ServerPlayer serverPlayer)) {
      return;
    }
    returnPackItems(serverPlayer);
    serverPlayer.setData(ModAttachments.ANE_PACK, AnePack.EMPTY);
  }

  public static void savePack(ServerPlayer player, AnePack pack) {
    player.setData(ModAttachments.ANE_PACK, pack);
  }

  public static AnePack getPack(Player player) {
    return player.getData(ModAttachments.ANE_PACK);
  }

  private static MenuProvider createMenuProvider(ServerPlayer player) {
    return new MenuProvider() {
      @Override
      public Component getDisplayName() {
        return Component.translatable("container.seroka.ane_pack");
      }

      @Override
      public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
        return new AnePackMenu(containerId, inventory, player);
      }
    };
  }

  private static void returnPackItems(ServerPlayer player) {
    for (ItemStack stack : getPack(player).slots()) {
      if (stack.isEmpty()) {
        continue;
      }
      if (!player.getInventory().add(stack.copy())) {
        player.drop(stack.copy(), false);
      }
    }
  }

  private static void setMultiplierModifier(
      Player player,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      ResourceLocation id,
      double amount
  ) {
    AttributeInstance instance = player.getAttribute(attribute);
    if (instance == null) {
      return;
    }
    instance.removeModifier(id);
    instance.addPermanentModifier(
        new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );
  }

  private static void setValueModifier(
      Player player,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      ResourceLocation id,
      double amount
  ) {
    AttributeInstance instance = player.getAttribute(attribute);
    if (instance == null) {
      return;
    }
    instance.removeModifier(id);
    instance.addPermanentModifier(
        new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE)
    );
  }

  private static void removeModifier(
      Player player,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      ResourceLocation id
  ) {
    AttributeInstance instance = player.getAttribute(attribute);
    if (instance != null) {
      instance.removeModifier(id);
    }
  }
}
