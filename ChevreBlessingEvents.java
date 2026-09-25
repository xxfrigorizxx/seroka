package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class ChevreBlessingEvents {

  private ChevreBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      ChevreBlessingHandler.tickCharge(player);
    }
  }

  @SubscribeEvent
  public static void onLivingFall(LivingFallEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (ChevreBlessingHandler.shouldNegateFallDamage(player, event.getDistance())) {
      event.setDistance(0.0F);
    }
  }

  @SubscribeEvent
  public static void onLivingDeath(LivingDeathEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      ChevreBlessingHandler.cancelCharge(player);
    }
  }
}
