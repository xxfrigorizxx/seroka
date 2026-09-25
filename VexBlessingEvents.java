package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Vex;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class VexBlessingEvents {

  private VexBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Pre event) {
    if (ChimereBlessingService.hasBlessing(event.getEntity(), GodIds.VEX)) {
      VexBlessingHandler.refreshIntangibilityPhysics(event.getEntity());
    }
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      VexBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onChangeTarget(LivingChangeTargetEvent event) {
    if (!(event.getEntity() instanceof Vex vex)) {
      return;
    }
    if (VexBlessingHandler.isProtectedFromVexes(event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
      if (vex.getTarget() != null
          && VexBlessingHandler.isProtectedFromVexes(vex.getTarget())) {
        vex.setTarget(null);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (VexBlessingHandler.isIntangible(player)) {
      event.setCanceled(true);
      return;
    }
    if (VexBlessingHandler.isProtectedFromVexes(player)
        && VexBlessingHandler.isVexDamage(event.getSource())) {
      event.setCanceled(true);
    }
  }
}
