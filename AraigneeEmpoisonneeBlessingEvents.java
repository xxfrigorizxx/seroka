package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.CaveSpider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class AraigneeEmpoisonneeBlessingEvents {

  private AraigneeEmpoisonneeBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      AraigneeEmpoisonneeBlessingHandler.refreshIfNeeded(player);
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
    AraigneeEmpoisonneeBlessingHandler.applyPoisonOnHit(attacker, event.getEntity());
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof CaveSpider caveSpider)) {
      return;
    }
    if (AraigneeEmpoisonneeBlessingHandler.isProtectedFromCaveSpiders(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (caveSpider.getTarget() != null
          && AraigneeEmpoisonneeBlessingHandler.isProtectedFromCaveSpiders(caveSpider.getTarget())) {
        caveSpider.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!AraigneeEmpoisonneeBlessingHandler.isProtectedFromCaveSpiders(event.getEntity())) {
      return;
    }
    if (isCaveSpiderAggressor(event.getSource())) {
      event.setCanceled(true);
    }
  }

  private static boolean isCaveSpiderAggressor(DamageSource source) {
    if (source.getEntity() instanceof CaveSpider) {
      return true;
    }
    return source.getDirectEntity() instanceof CaveSpider;
  }
}
