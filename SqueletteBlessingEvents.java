package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class SqueletteBlessingEvents {

  private SqueletteBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      SqueletteBlessingHandler.refreshIfNeeded(player);
      purgeUnauthorizedArc(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof AbstractSkeleton skeleton)) {
      return;
    }
    if (SqueletteBlessingHandler.isProtectedFromSkeletons(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (skeleton.getTarget() != null
          && SqueletteBlessingHandler.isProtectedFromSkeletons(skeleton.getTarget())) {
        skeleton.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!SqueletteBlessingHandler.isProtectedFromSkeletons(event.getEntity())) {
      return;
    }
    if (SqueletteBlessingHandler.isSkeletonAggressor(event.getSource())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
    DamageSource source = event.getSource();
    if (!(source.getEntity() instanceof ServerPlayer attacker)) {
      return;
    }
    if (!SqueletteBlessingHandler.shouldDoubleBowDamage(source, attacker)) {
      return;
    }
    event.setNewDamage(SqueletteBlessingHandler.applyBowDamageBonus(event.getNewDamage()));
  }

  @SubscribeEvent
  public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide()) {
      return;
    }

    if (event.getEntity() instanceof AbstractArrow arrow) {
      if (arrow.getOwner() instanceof ServerPlayer player) {
        ItemStack weapon = arrow.getWeaponItem();
        if (!weapon.isEmpty() && weapon.getItem() instanceof BowItem) {
          SqueletteBlessingHandler.onArrowShot(player);
        }
      }
      return;
    }

    if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
      return;
    }
    if (!(event.getLevel() instanceof ServerLevel level)) {
      return;
    }
    if (!ArcInfiniHelper.isArcInfini(itemEntity.getItem())) {
      return;
    }
    event.setCanceled(true);
  }

  @SubscribeEvent
  public static void onItemToss(ItemTossEvent event) {
    if (ArcInfiniHelper.isArcInfini(event.getEntity().getItem())) {
      event.setCanceled(true);
    }
  }

  private static void purgeUnauthorizedArc(ServerPlayer player) {
    boolean authorized = ChimereBlessingService.hasBlessing(player, GodIds.SQUELETTE);
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (ArcInfiniHelper.isArcInfini(stack) && !authorized) {
        ArcInfiniHelper.vanishAsAcid(player, stack);
      }
    }
    ItemStack carried = player.containerMenu.getCarried();
    if (ArcInfiniHelper.isArcInfini(carried) && !authorized) {
      player.containerMenu.setCarried(ItemStack.EMPTY);
      ArcInfiniHelper.vanishAsAcid(player, carried);
    }
  }
}
