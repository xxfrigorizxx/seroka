package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class ZombieBlessingEvents {

  private ZombieBlessingEvents() {}

  @SubscribeEvent
  public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ZombieBlessingHandler.hasZombieFamilyBlessing(player)) {
      return;
    }
    if (!ZombieBlessingHandler.isRottenFlesh(event.getItem())) {
      return;
    }
    ZombieBlessingHandler.beginEatingRottenFlesh(player);
  }

  @SubscribeEvent
  public static void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      ZombieBlessingHandler.endEatingRottenFlesh(player);
    }
  }

  @SubscribeEvent
  public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      ZombieBlessingHandler.endEatingRottenFlesh(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Zombie zombie)) {
      return;
    }
    if (ZombieBlessingHandler.isProtectedFromZombies(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (zombie.getTarget() != null && ZombieBlessingHandler.isProtectedFromZombies(zombie.getTarget())) {
        zombie.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!ZombieBlessingHandler.isProtectedFromZombies(event.getEntity())) {
      return;
    }
    if (event.getSource().getEntity() instanceof Zombie) {
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
    if (ZombieBlessingHandler.shouldBlockRottenFleshEffect(player, effect)) {
      event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
  }
}
