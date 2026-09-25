package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.monster.breeze.Breeze;

@EventBusSubscriber(modid = ModMain.MODID)
public final class BreezeBlessingEvents {

  private BreezeBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      BreezeBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Breeze breeze)) {
      return;
    }
    if (BreezeBlessingHandler.isProtectedFromBreezes(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (breeze.getTarget() != null
          && BreezeBlessingHandler.isProtectedFromBreezes(breeze.getTarget())) {
        breeze.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingFall(LivingFallEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (ChimereBlessingService.hasBlessing(player, GodIds.BREEZE)) {
      event.setDistance(0.0F);
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.BREEZE)) {
      return;
    }
    if (BreezeBlessingHandler.isBreezeDamage(event.getSource())
        || event.getSource().is(DamageTypes.FALL)) {
      event.setCanceled(true);
    }
  }
}
