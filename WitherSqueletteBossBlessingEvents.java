package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class WitherSqueletteBossBlessingEvents {

  private WitherSqueletteBossBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      WitherSqueletteBossBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (event.getEntity() instanceof AbstractSkeleton skeleton) {
      if (WitherSqueletteBossBlessingHandler.isProtectedFromSkeletons(event.getNewAboutToBeSetTarget())) {
        event.setNewAboutToBeSetTarget(null);
        if (skeleton.getTarget() != null
            && WitherSqueletteBossBlessingHandler.isProtectedFromSkeletons(skeleton.getTarget())) {
          skeleton.setTarget(null);
        }
      }
      return;
    }
    if (event.getEntity() instanceof WitherBoss wither) {
      if (WitherSqueletteBossBlessingHandler.isProtectedFromSkeletons(event.getNewAboutToBeSetTarget())) {
        event.setNewAboutToBeSetTarget(null);
        if (wither.getTarget() != null
            && WitherSqueletteBossBlessingHandler.isProtectedFromSkeletons(wither.getTarget())) {
          wither.setTarget(null);
        }
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.WITHER_SQUELETTE_BOSS)) {
      return;
    }
    if (WitherSqueletteBossBlessingHandler.isSkeletonAggressor(event.getSource())) {
      event.setCanceled(true);
      return;
    }
    if (WitherSqueletteBossBlessingHandler.isFallOrWitherDamage(event.getSource())) {
      event.setCanceled(true);
      return;
    }
    float reduced = WitherSqueletteBossBlessingHandler.reduceIncomingDamage(player, event.getAmount());
    event.setAmount(reduced);
  }
}
