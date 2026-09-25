package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ModMain.MODID)
public final class PoissonArgentBlessingEvents {

  private PoissonArgentBlessingEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      PoissonArgentBlessingHandler.refreshIfNeeded(player);
    }
  }

  @SubscribeEvent
  public static void onBlockBreak(BlockEvent.BreakEvent event) {
    if (event.getLevel().isClientSide()) {
      return;
    }
    BlockPos brokenPos = event.getPos();
    for (ServerPlayer player : event.getLevel().getServer().getPlayerList().getPlayers()) {
      if (!ChimereBlessingService.hasBlessing(player, GodIds.POISSON_ARGENT)) {
        continue;
      }
      BlockPos anchor = PoissonArgentBlessingHandler.getBuriedAnchor(player);
      if (anchor != null && anchor.equals(brokenPos)) {
        PoissonArgentBlessingHandler.onAnchorBlockBroken(player);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!PoissonArgentBlessingHandler.isBuried(player)) {
      return;
    }
    DamageSource source = event.getSource();
    if (source.is(DamageTypes.IN_WALL)) {
      event.setCanceled(true);
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
    if (!PoissonArgentBlessingHandler.isBuried(player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.POISSON_ARGENT)) {
      return;
    }
    DamageSource source = event.getSource();
    if (isExplosion(source) || isOtherPlayerAttack(source, player)) {
      PoissonArgentBlessingHandler.onForcedExit(player);
    }
  }

  private static boolean isExplosion(DamageSource source) {
    return source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION);
  }

  private static boolean isOtherPlayerAttack(DamageSource source, ServerPlayer victim) {
    return source.getEntity() instanceof Player attacker && attacker != victim;
  }
}
