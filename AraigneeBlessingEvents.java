package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Spider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class AraigneeBlessingEvents {

  private AraigneeBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      AraigneeBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Spider spider)) {
      return;
    }
    if (AraigneeBlessingHandler.isProtectedFromSpiders(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (spider.getTarget() != null && AraigneeBlessingHandler.isProtectedFromSpiders(spider.getTarget())) {
        spider.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!AraigneeBlessingHandler.isProtectedFromSpiders(event.getEntity())) {
      return;
    }
    if (isSpiderAggressor(event.getSource())) {
      event.setCanceled(true);
    }
  }

  private static boolean isSpiderAggressor(DamageSource source) {
    if (source.getEntity() instanceof Spider) {
      return true;
    }
    return source.getDirectEntity() instanceof Spider;
  }
}
