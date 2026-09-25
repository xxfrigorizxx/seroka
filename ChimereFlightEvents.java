package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Logique de vol côté serveur uniquement (le client utilise {@link com.seroka.client.ChimereFlightClientHandler}). */
@EventBusSubscriber(modid = ModMain.MODID)
public final class ChimereFlightEvents {

  private ChimereFlightEvents() {}

  @SubscribeEvent
  public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
    Player player = event.getEntity();
    if (player.level().isClientSide()) {
      return;
    }
    ChimereFlightHelper.ensureVanillaCreativeFlight(player);
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }
    ChimereFlightHelper.tickFlight(player);
    ChimereFlightHelper.tickFallProtection(player);
  }

  @SubscribeEvent
  public static void onPlayerTickPost(PlayerTickEvent.Post event) {
    Player player = event.getEntity();
    if (player.level().isClientSide()) {
      return;
    }
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }
    ChimereFlightHelper.trackFlightTransition(player);
    ChimereFlightHelper.tickFallProtection(player);
  }

  @SubscribeEvent
  public static void onLivingFall(LivingFallEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (ChimereFlightHelper.shouldNegateFallDamage(player)) {
      event.setDistance(0.0F);
      player.fallDistance = 0.0F;
      ChimereFlightHelper.markFlightLanding(player);
      ChimereFlightHelper.clearSafeLanding(player);
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!event.getSource().is(DamageTypes.FALL)) {
      return;
    }
    if (ChimereFlightHelper.shouldNegateFallDamage(player)) {
      event.setCanceled(true);
      player.fallDistance = 0.0F;
      ChimereFlightHelper.markFlightLanding(player);
      ChimereFlightHelper.clearSafeLanding(player);
    }
  }
}
