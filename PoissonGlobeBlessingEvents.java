package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class PoissonGlobeBlessingEvents {

  private PoissonGlobeBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    Player player = event.getEntity();
    PoissonGlobeBlessingHandler.refreshIfNeeded(player);
  }
}
