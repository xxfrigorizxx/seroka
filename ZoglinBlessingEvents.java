package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class ZoglinBlessingEvents {

  private ZoglinBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      ZoglinBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Hoglin hoglin)) {
      return;
    }
    if (ZoglinBlessingHandler.isProtectedFromHoglins(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (hoglin.getTarget() != null
          && ZoglinBlessingHandler.isProtectedFromHoglins(hoglin.getTarget())) {
        hoglin.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ZOGLIN)) {
      return;
    }
    if (ZoglinBlessingHandler.isFireOrLavaDamage(event.getSource())) {
      event.setCanceled(true);
      return;
    }
    if (event.getSource().getEntity() instanceof Hoglin) {
      event.setCanceled(true);
    }
  }
}
