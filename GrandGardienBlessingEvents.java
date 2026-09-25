package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class GrandGardienBlessingEvents {

  private GrandGardienBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      GrandGardienBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (GardienBlessingHandler.isProtectedFromGuardians(player)
        && GardienBlessingHandler.isGuardianDamage(event.getSource())) {
      event.setCanceled(true);
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GRAND_GARDIEN)) {
      return;
    }
    float reduced = GrandGardienBlessingHandler.reduceIncomingDamage(player, event.getAmount());
    event.setAmount(reduced);
  }

  @SubscribeEvent
  public static void onLivingDamagePost(LivingDamageEvent.Post event) {
    if (event.getNewDamage() <= 0.0F) {
      return;
    }
    if (!(event.getEntity() instanceof ServerPlayer victim)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(victim, GodIds.GRAND_GARDIEN)) {
      return;
    }
    if (event.getSource().is(DamageTypes.THORNS)) {
      return;
    }
    if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim) {
      return;
    }
    GrandGardienBlessingHandler.applyThornsReflect(victim, attacker);
  }
}
