package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class GolemFerBlessingEvents {

  private GolemFerBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      GolemFerBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof IronGolem golem)) {
      return;
    }
    if (GolemFerBlessingHandler.isProtectedFromIronGolems(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (golem.getTarget() != null
          && GolemFerBlessingHandler.isProtectedFromIronGolems(golem.getTarget())) {
        golem.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (GolemFerBlessingHandler.isProtectedFromIronGolems(player)
        && GolemFerBlessingHandler.isIronGolemDamage(event.getSource())) {
      event.setCanceled(true);
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GOLEM_FER)) {
      return;
    }
    float reduced = GolemFerBlessingHandler.reduceIncomingDamage(player, event.getAmount());
    event.setAmount(reduced);
  }
}
