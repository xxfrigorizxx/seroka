package com.seroka.client;

import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Remplace l'inventaire vanilla par {@link FrontalierInventoryScreen} pour les Frontaliers.
 */
@EventBusSubscriber(modid = com.seroka.ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierInventoryHandler {

  private FrontalierInventoryHandler() {}

  @SubscribeEvent
  public static void onScreenOpening(ScreenEvent.Opening event) {
    if (!isLocalFrontalier()) {
      return;
    }
    Screen newScreen = event.getNewScreen();
    if (newScreen instanceof InventoryScreen && !(newScreen instanceof FrontalierInventoryScreen)) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
        event.setNewScreen(new FrontalierInventoryScreen(player));
      }
    }
  }

  private static boolean isLocalFrontalier() {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null) {
      return false;
    }
    return player.getData(ModAttachments.PLAYER_FACTION).isFrontalier();
  }
}
