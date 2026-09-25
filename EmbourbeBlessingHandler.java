package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Embourbé : vision nocturne, immunité au poison, arc et flèches empoisonnées. */
public final class EmbourbeBlessingHandler {

  private static final int POISON_ARROW_COOLDOWN_TICKS = 2 * 60 * 20;
  private static final int POISON_ARROW_COUNT = 20;
  private static final int POISON_DURATION_TICKS = 10 * 20;
  private static final double BOGGED_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final Map<UUID, Long> POISON_ARROW_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Integer> POISON_ARROWS_BEFORE_SHOT = new ConcurrentHashMap<>();

  private EmbourbeBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EMBOURBE)) {
      return;
    }
    setNightVision(player, true);
    ensureIncarnationBow(player);
    clearBoggedAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EMBOURBE)) {
      return;
    }
    if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
      setNightVision(player, true);
    }
    if (player.tickCount % 40 == 0) {
      ensureIncarnationBow(player);
    }
    FlecheEmpoisonneeHelper.purgeOutsidePlayerInventory(player);
  }

  public static void activatePoisonArrows(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EMBOURBE)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - POISON_ARROW_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < POISON_ARROW_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((POISON_ARROW_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.embourbe.poison_arrows.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    ItemStack arrows = FlecheEmpoisonneeHelper.createStack(POISON_ARROW_COUNT);
    if (!player.getInventory().add(arrows)) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.embourbe.poison_arrows.inventory_full")
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    POISON_ARROW_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.8F, 0.9F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.embourbe.poison_arrows.granted", POISON_ARROW_COUNT)
            .withStyle(ChatFormatting.GREEN),
        true
    );
  }

  public static boolean shouldBlockPoisonEffect(Player player, MobEffectInstance effect) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EMBOURBE)) {
      return false;
    }
    return effect.is(MobEffects.POISON);
  }

  public static void beginBowUse(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EMBOURBE)) {
      return;
    }
    POISON_ARROWS_BEFORE_SHOT.put(player.getUUID(), countPoisonArrows(player));
  }

  public static void tagPoisonArrowIfNeeded(AbstractArrow arrow) {
    if (arrow.getOwner() instanceof ServerPlayer shooter) {
      tagPoisonArrowIfNeeded(shooter, arrow);
    }
  }

  public static void tagPoisonArrowIfNeeded(ServerPlayer shooter, AbstractArrow arrow) {
    if (!ChimereBlessingService.hasBlessing(shooter, GodIds.EMBOURBE)) {
      return;
    }
    int before = POISON_ARROWS_BEFORE_SHOT.getOrDefault(shooter.getUUID(), countPoisonArrows(shooter));
    int after = countPoisonArrows(shooter);
    if (after < before) {
      arrow.getPersistentData().putBoolean(FlecheEmpoisonneeHelper.POISON_ARROW_PROJECTILE_TAG, true);
    }
    POISON_ARROWS_BEFORE_SHOT.put(shooter.getUUID(), after);
  }

  public static void applyPoisonArrowHit(LivingEntity target, AbstractArrow arrow) {
    if (arrow.getPersistentData().getBoolean(FlecheEmpoisonneeHelper.POISON_ARROW_PROJECTILE_TAG)) {
      target.addEffect(new MobEffectInstance(
          MobEffects.POISON,
          POISON_DURATION_TICKS,
          0,
          false,
          true,
          true
      ));
    }
  }

  public static boolean isProtectedFromBogged(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.EMBOURBE);
  }

  public static boolean isBoggedAggressor(DamageSource source) {
    if (source.getEntity() instanceof Bogged) {
      return true;
    }
    if (source.getDirectEntity() instanceof AbstractArrow arrow) {
      return arrow.getOwner() instanceof Bogged;
    }
    return false;
  }

  public static void clearBoggedAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EMBOURBE)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(BOGGED_AGGRO_CLEAR_RADIUS);
    for (Bogged bogged : player.serverLevel().getEntitiesOfClass(Bogged.class, area)) {
      if (bogged.getTarget() == player) {
        bogged.setTarget(null);
      }
      if (bogged.getLastHurtByMob() == player) {
        bogged.setLastHurtByMob(null);
      }
    }
  }

  public static void clear(Player player) {
    POISON_ARROW_COOLDOWN_LAST_TICK.remove(player.getUUID());
    POISON_ARROWS_BEFORE_SHOT.remove(player.getUUID());
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
      FlecheEmpoisonneeHelper.purgeAllPoisonArrows(serverPlayer);
    }
  }

  private static int countPoisonArrows(ServerPlayer player) {
    int count = 0;
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (FlecheEmpoisonneeHelper.isPoisonArrow(stack)) {
        count += stack.getCount();
      }
    }
    ItemStack carried = player.containerMenu.getCarried();
    if (FlecheEmpoisonneeHelper.isPoisonArrow(carried)) {
      count += carried.getCount();
    }
    return count;
  }

  private static void ensureIncarnationBow(ServerPlayer player) {
    if (hasBow(player)) {
      return;
    }
    ItemStack bow = new ItemStack(Items.BOW);
    if (!player.getInventory().add(bow)) {
      player.drop(bow, false);
    }
  }

  private static boolean hasBow(Player player) {
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (!stack.isEmpty() && stack.getItem() instanceof BowItem) {
        return true;
      }
    }
    return player.getMainHandItem().getItem() instanceof BowItem
        || player.getOffhandItem().getItem() instanceof BowItem;
  }

  private static void setNightVision(ServerPlayer player, boolean enabled) {
    if (!enabled) {
      player.removeEffect(MobEffects.NIGHT_VISION);
      return;
    }
    player.addEffect(new MobEffectInstance(
        MobEffects.NIGHT_VISION,
        MobEffectInstance.INFINITE_DURATION,
        0,
        false,
        false,
        true
    ));
  }
}
