package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Slime;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class SlimeBlessingEvents {

  private SlimeBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      SlimeBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Slime slime)) {
      return;
    }
    if (SlimeBlessingHandler.isProtectedFromSlimes(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (slime.getTarget() != null
          && SlimeBlessingHandler.isProtectedFromSlimes(slime.getTarget())) {
        slime.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingFall(LivingFallEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (ChimereBlessingService.hasBlessing(player, GodIds.SLIME)) {
      event.setDistance(0.0F);
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SLIME)) {
      return;
    }
    if (SlimeBlessingHandler.isFallDamage(event.getSource())) {
      event.setCanceled(true);
      return;
    }
    if (SlimeBlessingHandler.isSlimeDamage(event.getSource())) {
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
    float boosted = SlimeBlessingHandler.applyHealthScaledDamageBonus(attacker, event.getNewDamage());
    event.setNewDamage(boosted);
  }
}
