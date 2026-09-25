package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class CreeperBlessingEvents {

  private CreeperBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      CreeperBlessingHandler.refreshIfNeeded(player);
      CreeperChargeBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onLivingDeath(LivingDeathEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      CreeperBlessingHandler.cancelPending(player);
      CreeperChargeBlessingHandler.cancelPending(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Creeper creeper)) {
      return;
    }
    if (CreeperBlessingHandler.isProtectedFromCreepers(event.getNewAboutToBeSetTarget())
        || CreeperChargeBlessingHandler.isProtectedFromCreepers(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (creeper.getTarget() != null
          && (CreeperBlessingHandler.isProtectedFromCreepers(creeper.getTarget())
          || CreeperChargeBlessingHandler.isProtectedFromCreepers(creeper.getTarget()))) {
        creeper.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (hasExplosionImmunity(player) && isOwnExplosionDamage(event.getSource(), player)) {
      event.setCanceled(true);
      return;
    }
    if (isProtectedFromCreepers(player) && isCreeperAggressor(event.getSource())) {
      event.setCanceled(true);
    }
  }

  private static boolean hasExplosionImmunity(ServerPlayer player) {
    return CreeperBlessingHandler.hasExplosionImmunity(player)
        || CreeperChargeBlessingHandler.hasExplosionImmunity(player);
  }

  private static boolean isProtectedFromCreepers(ServerPlayer player) {
    return CreeperBlessingHandler.isProtectedFromCreepers(player)
        || CreeperChargeBlessingHandler.isProtectedFromCreepers(player);
  }

  private static boolean isOwnExplosionDamage(DamageSource source, ServerPlayer player) {
    if (source.getEntity() != player) {
      return false;
    }
    return source.is(DamageTypes.PLAYER_EXPLOSION) || source.is(DamageTypes.EXPLOSION);
  }

  private static boolean isCreeperAggressor(DamageSource source) {
    if (source.getEntity() instanceof Creeper) {
      return true;
    }
    return source.getDirectEntity() instanceof Creeper;
  }
}
