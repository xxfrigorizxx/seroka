package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Wolf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class LamaBlessingEvents {

  private LamaBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      LamaBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Wolf wolf)) {
      return;
    }
    if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player
        && LamaBlessingHandler.isProtectedFromWolves(player)) {
      event.setNewAboutToBeSetTarget(null);
      if (wolf.getTarget() instanceof ServerPlayer current
          && LamaBlessingHandler.isProtectedFromWolves(current)) {
        wolf.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!LamaBlessingHandler.isProtectedFromWolves(player)) {
      return;
    }
    if (isWolfAggressor(event.getSource())) {
      event.setCanceled(true);
    }
  }

  private static boolean isWolfAggressor(DamageSource source) {
    if (source.getEntity() instanceof Wolf) {
      return true;
    }
    return source.getDirectEntity() instanceof Wolf;
  }
}
