package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.EnderMan;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.EnderManAngerEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class EndermanBlessingEvents {

  private EndermanBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      EndermanBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onEnderManAnger(EnderManAngerEvent event) {
    if (EndermanBlessingHandler.isProtectedFromEndermen(event.getPlayer())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof EnderMan enderman)) {
      return;
    }
    if (EndermanBlessingHandler.isProtectedFromEndermen(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (enderman.getTarget() != null
          && EndermanBlessingHandler.isProtectedFromEndermen(enderman.getTarget())) {
        enderman.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (EndermanBlessingHandler.isProtectedFromEndermen(player)
        && EndermanBlessingHandler.isEndermanDamage(event.getSource())) {
      event.setCanceled(true);
    }
  }
}
