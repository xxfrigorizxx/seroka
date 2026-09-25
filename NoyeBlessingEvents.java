package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Drowned;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class NoyeBlessingEvents {

  private NoyeBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      NoyeBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.NOYE)) {
      return;
    }
    if (!NoyeBlessingHandler.isRottenFlesh(event.getItem())) {
      return;
    }
    NoyeBlessingHandler.beginEatingRottenFlesh(player);
  }

  @SubscribeEvent
  public static void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      NoyeBlessingHandler.endEatingRottenFlesh(player);
    }
  }

  @SubscribeEvent
  public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      NoyeBlessingHandler.endEatingRottenFlesh(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Drowned drowned)) {
      return;
    }
    if (NoyeBlessingHandler.isProtectedFromDrowned(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (drowned.getTarget() != null && NoyeBlessingHandler.isProtectedFromDrowned(drowned.getTarget())) {
        drowned.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!NoyeBlessingHandler.isProtectedFromDrowned(event.getEntity())) {
      return;
    }
    if (event.getSource().getEntity() instanceof Drowned) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onEffectApplicable(MobEffectEvent.Applicable event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    MobEffectInstance effect = event.getEffectInstance();
    if (effect == null) {
      return;
    }
    if (NoyeBlessingHandler.shouldBlockRottenFleshEffect(player, effect)) {
      event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
  }
}
