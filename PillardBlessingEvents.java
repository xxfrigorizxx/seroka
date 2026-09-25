package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Pillager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class PillardBlessingEvents {

  private PillardBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      PillardBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Pillager pillager)) {
      return;
    }
    if (PillardBlessingHandler.isProtectedFromPillagers(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (pillager.getTarget() != null
          && PillardBlessingHandler.isProtectedFromPillagers(pillager.getTarget())) {
        pillager.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!PillardBlessingHandler.isProtectedFromPillagers(event.getEntity())) {
      return;
    }
    if (event.getSource().getEntity() instanceof Pillager) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ItemEntity itemEntity) {
      if (FlechePillardHelper.isPillardArrow(itemEntity.getItem())) {
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent
  public static void onItemToss(ItemTossEvent event) {
    if (FlechePillardHelper.isPillardArrow(event.getEntity().getItem())) {
      event.setCanceled(true);
      if (event.getPlayer() instanceof ServerPlayer player) {
        FlechePillardHelper.purgeOutsidePlayerInventory(player);
      }
    }
  }
}
