package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class EvocateurBlessingEvents {

  private EvocateurBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      EvocateurBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onLivingDeath(LivingDeathEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EVOCATEUR)) {
      return;
    }
    if (EvocateurBlessingHandler.tryUseTotem(player)) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      EvocateurBlessingHandler.resetTotemForLife(player);
    }
  }

  @SubscribeEvent
  public static void onLivingDamagePost(LivingDamageEvent.Post event) {
    if (event.getNewDamage() <= 0.0F) {
      return;
    }
    if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
      return;
    }
    if (!(event.getEntity() instanceof LivingEntity target)) {
      return;
    }
    EvocateurBlessingHandler.onOwnerAttack(attacker, target);
  }
}
