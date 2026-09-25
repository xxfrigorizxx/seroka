package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class EnderDragonBlessingEvents {

  private EnderDragonBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      EnderDragonBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof EnderDragon dragon)) {
      return;
    }
    if (EnderDragonBlessingHandler.isProtectedFromDragon(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (dragon.getTarget() != null
          && EnderDragonBlessingHandler.isProtectedFromDragon(dragon.getTarget())) {
        dragon.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ENDER_DRAGON)) {
      return;
    }
    if (EnderDragonBlessingHandler.isImmuneToBreathDamage(player, event.getSource())) {
      event.setCanceled(true);
      return;
    }
    float reduced = EnderDragonBlessingHandler.reduceIncomingDamage(player, event.getAmount());
    event.setAmount(reduced);
  }

  @SubscribeEvent
  public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
    if (event.getNewDamage() <= 0.0F) {
      return;
    }
    if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
      return;
    }
    float boosted = EnderDragonBlessingHandler.boostOutgoingDamage(attacker, event.getNewDamage());
    event.setNewDamage(boosted);
  }
}
