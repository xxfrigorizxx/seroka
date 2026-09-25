package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class ArpenteurBlessingEvents {

  private ArpenteurBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      ArpenteurBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
    if (event.getNewDamage() <= 0.0F) {
      return;
    }
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ARPENTEUR)) {
      return;
    }
    if (ArpenteurBlessingHandler.isThermalDamage(event.getSource())) {
      event.setNewDamage(0.0F);
    }
  }
}
