package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Stray;
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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class VagabondBlessingEvents {

  private VagabondBlessingEvents() {}

  @SubscribeEvent
  public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (event.getItem().getItem() instanceof BowItem) {
      VagabondBlessingHandler.beginBowUse(player);
    }
  }

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      VagabondBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Stray stray)) {
      return;
    }
    if (VagabondBlessingHandler.isProtectedFromStrays(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (stray.getTarget() != null && VagabondBlessingHandler.isProtectedFromStrays(stray.getTarget())) {
        stray.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!VagabondBlessingHandler.isProtectedFromStrays(event.getEntity())) {
      return;
    }
    if (VagabondBlessingHandler.isStrayAggressor(event.getSource())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
    DamageSource source = event.getSource();
    if (source.getEntity() instanceof ServerPlayer attacker
        && VagabondBlessingHandler.shouldDoubleBowDamage(source, attacker)) {
      event.setNewDamage(VagabondBlessingHandler.applyBowDamageBonus(event.getNewDamage()));
      return;
    }

    if (event.getNewDamage() <= 0.0F) {
      return;
    }
    if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) {
      return;
    }
    VagabondBlessingHandler.applySlowArrowHit(event.getEntity(), arrow);
  }

  @SubscribeEvent
  public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide()) {
      return;
    }

    if (event.getEntity() instanceof AbstractArrow arrow) {
      VagabondBlessingHandler.tagSlowArrowIfNeeded(arrow);
      return;
    }

    if (event.getEntity() instanceof ItemEntity itemEntity) {
      ItemStack stack = itemEntity.getItem();
      if (FlecheLenteurHelper.isSlowArrow(stack)) {
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent
  public static void onItemToss(ItemTossEvent event) {
    if (FlecheLenteurHelper.isSlowArrow(event.getEntity().getItem())) {
      event.setCanceled(true);
      if (event.getPlayer() instanceof ServerPlayer player) {
        FlecheLenteurHelper.purgeOutsidePlayerInventory(player);
      }
    }
  }
}
