package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Phantom;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class PhantomeBlessingEvents {

  private PhantomeBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      PhantomeBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
    if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
      return;
    }
    float boosted = PhantomeBlessingHandler.applyFlightSpeedDamage(attacker, event.getNewDamage());
    event.setNewDamage(boosted);
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Phantom phantom)) {
      return;
    }
    if (PhantomeBlessingHandler.isProtectedFromPhantoms(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (phantom.getTarget() != null
          && PhantomeBlessingHandler.isProtectedFromPhantoms(phantom.getTarget())) {
        phantom.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!PhantomeBlessingHandler.isProtectedFromPhantoms(event.getEntity())) {
      return;
    }
    if (isPhantomAggressor(event.getSource())) {
      event.setCanceled(true);
    }
  }

  private static boolean isPhantomAggressor(DamageSource source) {
    if (source.getEntity() instanceof Phantom) {
      return true;
    }
    return source.getDirectEntity() instanceof Phantom;
  }
}
