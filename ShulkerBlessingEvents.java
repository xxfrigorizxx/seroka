package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Shulker;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class ShulkerBlessingEvents {

  private ShulkerBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      ShulkerBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Shulker shulker)) {
      return;
    }
    if (ShulkerBlessingHandler.isProtectedFromShulkers(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (shulker.getTarget() != null
          && ShulkerBlessingHandler.isProtectedFromShulkers(shulker.getTarget())) {
        shulker.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (ShulkerBlessingHandler.isProtectedFromShulkers(player)
        && ShulkerBlessingHandler.isShulkerDamage(event.getSource())) {
      event.setCanceled(true);
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SHULKER)) {
      return;
    }
    float reduced = ShulkerBlessingHandler.reduceIncomingDamage(player, event.getAmount());
    event.setAmount(reduced);
  }
}
