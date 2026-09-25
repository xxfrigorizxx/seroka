package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class TortueBlessingCombatEvents {

  private TortueBlessingCombatEvents() {}

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.TORTUE)) {
      return;
    }
    float reduced = TortueBlessingHandler.applyShellDamageReduction(player, event.getAmount());
    event.setAmount(reduced);
  }
}
