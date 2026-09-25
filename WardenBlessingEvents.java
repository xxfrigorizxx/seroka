package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class WardenBlessingEvents {

  private WardenBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      WardenBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Warden warden)) {
      return;
    }
    if (WardenBlessingHandler.isProtectedFromWardens(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (warden.getTarget() != null
          && WardenBlessingHandler.isProtectedFromWardens(warden.getTarget())) {
        warden.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (WardenBlessingHandler.isProtectedFromWardens(player)
        && WardenBlessingHandler.isWardenDamage(event.getSource())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
    if (event.getNewDamage() <= 0.0F) {
      return;
    }
    if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
      return;
    }
    WardenBlessingHandler.recordStrikeDamage(attacker, event.getNewDamage());
  }

  @SubscribeEvent
  public static void onLivingDamagePost(LivingDamageEvent.Post event) {
    if (event.getNewDamage() <= 0.0F) {
      return;
    }
    if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
      return;
    }
    WardenBlessingHandler.applyArmorPierceFollowUp(attacker, event.getEntity(), event.getNewDamage());
  }
}
