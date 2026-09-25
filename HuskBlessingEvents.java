package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class HuskBlessingEvents {

  private HuskBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      HuskBlessingHandler.refreshIfNeeded(player);
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
    LivingEntity target = event.getEntity();
    HuskBlessingHandler.applyHungerOnHit(attacker, target);
  }

  @SubscribeEvent
  public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.HUSK)) {
      return;
    }
    if (!HuskBlessingHandler.isRottenFlesh(event.getItem())) {
      return;
    }
    HuskBlessingHandler.beginEatingRottenFlesh(player);
  }

  @SubscribeEvent
  public static void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      HuskBlessingHandler.endEatingRottenFlesh(player);
    }
  }

  @SubscribeEvent
  public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      HuskBlessingHandler.endEatingRottenFlesh(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Husk husk)) {
      return;
    }
    if (HuskBlessingHandler.isProtectedFromHusks(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (husk.getTarget() != null && HuskBlessingHandler.isProtectedFromHusks(husk.getTarget())) {
        husk.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!HuskBlessingHandler.isProtectedFromHusks(event.getEntity())) {
      return;
    }
    if (event.getSource().getEntity() instanceof Husk) {
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
    if (HuskBlessingHandler.shouldBlockRottenFleshEffect(player, effect)) {
      event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
  }
}
