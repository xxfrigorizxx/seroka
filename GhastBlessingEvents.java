package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class GhastBlessingEvents {

  private GhastBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      GhastBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Ghast ghast)) {
      return;
    }
    if (GhastBlessingHandler.isProtectedFromGhasts(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (ghast.getTarget() != null
          && GhastBlessingHandler.isProtectedFromGhasts(ghast.getTarget())) {
        ghast.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!GhastBlessingHandler.isProtectedFromGhasts(event.getEntity())) {
      return;
    }
    if (isGhastAggressor(event.getSource()) || GhastBlessingHandler.isFireOrLavaDamage(event.getSource())) {
      event.setCanceled(true);
    }
  }

  private static boolean isGhastAggressor(DamageSource source) {
    if (source.getEntity() instanceof Ghast) {
      return true;
    }
    if (source.getDirectEntity() instanceof LargeFireball fireball) {
      return fireball.getOwner() instanceof Ghast;
    }
    return false;
  }
}
