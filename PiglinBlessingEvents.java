package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class PiglinBlessingEvents {

  private PiglinBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      PiglinBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onLivingDeath(LivingDeathEvent event) {
    if (!(event.getEntity() instanceof Hoglin)) {
      return;
    }
    if (event.getSource().getEntity() instanceof ServerPlayer killer) {
      PiglinBlessingHandler.onHoglinKilled(killer);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Piglin piglin)) {
      return;
    }
    if (PiglinBlessingHandler.isProtectedFromPiglins(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (piglin.getTarget() != null
          && PiglinBlessingHandler.isProtectedFromPiglins(piglin.getTarget())) {
        piglin.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN)) {
      return;
    }
    if (PiglinBlessingHandler.isFireOrLavaDamage(event.getSource())) {
      event.setCanceled(true);
      return;
    }
    if (event.getSource().getEntity() instanceof Piglin) {
      event.setCanceled(true);
    }
  }
}
