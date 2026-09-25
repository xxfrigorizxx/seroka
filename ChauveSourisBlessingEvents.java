package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class ChauveSourisBlessingEvents {

  private ChauveSourisBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      ChauveSourisBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onLivingDamagePost(LivingDamageEvent.Post event) {
    if (event.getNewDamage() <= 0.0F) {
      return;
    }
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CHAUVE_SOURIS)) {
      return;
    }
    ChauveSourisBlessingHandler.onDamageTaken(player);
  }
}
