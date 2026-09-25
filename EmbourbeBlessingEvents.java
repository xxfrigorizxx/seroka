package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class EmbourbeBlessingEvents {

  private EmbourbeBlessingEvents() {}

  @SubscribeEvent
  public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (event.getItem().getItem() instanceof BowItem) {
      EmbourbeBlessingHandler.beginBowUse(player);
    }
  }

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      EmbourbeBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Bogged bogged)) {
      return;
    }
    if (EmbourbeBlessingHandler.isProtectedFromBogged(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (bogged.getTarget() != null && EmbourbeBlessingHandler.isProtectedFromBogged(bogged.getTarget())) {
        bogged.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!EmbourbeBlessingHandler.isProtectedFromBogged(event.getEntity())) {
      return;
    }
    if (EmbourbeBlessingHandler.isBoggedAggressor(event.getSource())) {
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
    if (EmbourbeBlessingHandler.shouldBlockPoisonEffect(player, effect)) {
      event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
  }

  @SubscribeEvent
  public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
    if (event.getNewDamage() <= 0.0F) {
      return;
    }
    if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
      return;
    }
    EmbourbeBlessingHandler.applyPoisonArrowHit(event.getEntity(), arrow);
  }

  @SubscribeEvent
  public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide()) {
      return;
    }

    if (event.getEntity() instanceof AbstractArrow arrow) {
      EmbourbeBlessingHandler.tagPoisonArrowIfNeeded(arrow);
      return;
    }

    if (event.getEntity() instanceof ItemEntity itemEntity) {
      ItemStack stack = itemEntity.getItem();
      if (FlecheEmpoisonneeHelper.isPoisonArrow(stack)) {
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent
  public static void onItemToss(ItemTossEvent event) {
    if (FlecheEmpoisonneeHelper.isPoisonArrow(event.getEntity().getItem())) {
      event.setCanceled(true);
      if (event.getPlayer() instanceof ServerPlayer player) {
        FlecheEmpoisonneeHelper.purgeOutsidePlayerInventory(player);
      }
    }
  }
}
