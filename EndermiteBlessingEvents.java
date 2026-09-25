package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Endermite;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class EndermiteBlessingEvents {

  private EndermiteBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      EndermiteBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Endermite endermite)) {
      return;
    }
    if (EndermiteBlessingHandler.isProtectedFromEndermites(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (endermite.getTarget() != null
          && EndermiteBlessingHandler.isProtectedFromEndermites(endermite.getTarget())) {
        endermite.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (EndermiteBlessingHandler.isProtectedFromEndermites(player)
        && EndermiteBlessingHandler.isEndermiteDamage(event.getSource())) {
      event.setCanceled(true);
    }
  }
}
