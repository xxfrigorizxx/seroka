package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class PiglinBruteBlessingEvents {

  private PiglinBruteBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      PiglinBruteBlessingHandler.refreshIfNeeded(player);
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
    float boosted = PiglinBruteBlessingHandler.applyAxeDamageBonus(attacker, event.getNewDamage());
    event.setNewDamage(boosted);
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!isPiglinMob(event.getEntity())) {
      return;
    }
    if (PiglinBruteBlessingHandler.isProtectedFromPiglins(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (event.getEntity() instanceof net.minecraft.world.entity.Mob mob
          && mob.getTarget() != null
          && PiglinBruteBlessingHandler.isProtectedFromPiglins(mob.getTarget())) {
        mob.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!PiglinBruteBlessingHandler.isProtectedFromPiglins(event.getEntity())) {
      return;
    }
    if (isPiglinMob(event.getSource().getEntity())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onItemToss(ItemTossEvent event) {
    if (PiglinBruteAxeHelper.isBruteAxe(event.getEntity().getItem())) {
      event.setCanceled(true);
      if (event.getPlayer() instanceof ServerPlayer player) {
        PiglinBruteAxeHelper.purgeAllBruteAxes(player);
      }
    }
  }

  @SubscribeEvent
  public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ItemEntity itemEntity) {
      if (PiglinBruteAxeHelper.isBruteAxe(itemEntity.getItem())) {
        event.setCanceled(true);
      }
    }
  }

  private static boolean isPiglinMob(net.minecraft.world.entity.Entity entity) {
    return entity instanceof Piglin || entity instanceof PiglinBrute || entity instanceof ZombifiedPiglin;
  }
}
