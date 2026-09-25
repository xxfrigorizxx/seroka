package com.seroka.client;

import com.seroka.ModMain;
import com.seroka.chimere.AraigneeWallClimbHelper;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Maintient la grimpe murale côté client (mouvement prédit localement). */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class AraigneeBlessingClientHandler {

  private AraigneeBlessingClientHandler() {}

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null) {
      return;
    }
    AraigneeWallClimbHelper.refresh(minecraft.player);
  }
}
