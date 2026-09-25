package com.seroka.chimere;

import com.seroka.ModMain;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class ChimereBlessingEvents {

  private ChimereBlessingEvents() {}

  @SubscribeEvent
  public static void onServerStarted(ServerStartedEvent event) {
    ChimereGodsBootstrap.init();
    GodBlessingRegistry registry = GodBlessingRegistry.get(event.getServer());
    registry.reconcile();

    ModMain.LOGGER.info("[Seroka] Catalogue Chimère : {} dieu(x) enregistré(s)", GodCatalog.ALL.size());
    for (GodDefinition god : GodCatalog.ALL) {
      int active = registry.getActiveCount(god.id());
      ModMain.LOGGER.info("[Seroka] Quota {} : {}/{} place(s) utilisée(s)",
          god.id(), active, god.maxBlessings());
    }
  }
}
