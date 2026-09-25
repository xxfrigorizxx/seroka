package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Blaze;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class BlazeBlessingEvents {

  private BlazeBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      BlazeBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Blaze blaze)) {
      return;
    }
    if (BlazeBlessingHandler.isProtectedFromBlazes(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (blaze.getTarget() != null
          && BlazeBlessingHandler.isProtectedFromBlazes(blaze.getTarget())) {
        blaze.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.BLAZE)) {
      return;
    }
    if (BlazeBlessingHandler.isFireOrLavaDamage(event.getSource())
        || BlazeBlessingHandler.isBlazeProjectile(event.getSource())) {
      event.setCanceled(true);
    }
  }
}
