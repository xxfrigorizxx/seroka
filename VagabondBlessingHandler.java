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
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Vagabond : vision nocturne, arc, flèches de lenteur et immunité aux vagabonds. */
public final class VagabondBlessingHandler {

  private static final int SLOW_ARROW_COOLDOWN_TICKS = 2 * 60 * 20;
  private static final int SLOW_ARROW_COUNT = 10;
  private static final int SLOWNESS_DURATION_TICKS = 30 * 20;
  private static final double STRAY_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final Map<UUID, Long> SLOW_ARROW_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Integer> SLOW_ARROWS_BEFORE_SHOT = new ConcurrentHashMap<>();

  private VagabondBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VAGABOND)) {
      return;
    }
    refreshNightVision(player);
    ensureIncarnationBow(player);
    clearStrayAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VAGABOND)) {
      return;
    }
    refreshNightVision(player);
    if (player.tickCount % 40 == 0) {
      ensureIncarnationBow(player);
    }
    FlecheLenteurHelper.purgeOutsidePlayerInventory(player);
  }

  public static void activateSlowArrows(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VAGABOND)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - SLOW_ARROW_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < SLOW_ARROW_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((SLOW_ARROW_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.vagabond.slow_arrows.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    SLOW_ARROW_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    ItemStack arrows = FlecheLenteurHelper.createStack(SLOW_ARROW_COUNT);
    if (!player.getInventory().add(arrows)) {
      SLOW_ARROW_COOLDOWN_LAST_TICK.remove(player.getUUID());
      player.displayClientMessage(
          Component.translatable("blessing.seroka.vagabond.slow_arrows.inventory_full")
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }
    player.playNotifySound(SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.8F, 0.7F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.vagabond.slow_arrows.granted", SLOW_ARROW_COUNT)
            .withStyle(ChatFormatting.AQUA),
        true
    );
  }

  public static boolean shouldDoubleBowDamage(DamageSource source, ServerPlayer attacker) {
    if (!ChimereBlessingService.hasBlessing(attacker, GodIds.VAGABOND)) {
      return false;
    }
    if (!(source.getDirectEntity() instanceof AbstractArrow)) {
      return false;
    }
    if (source.getEntity() != attacker) {
      return false;
    }
    ItemStack weapon = source.getWeaponItem();
    return !weapon.isEmpty() && weapon.getItem() instanceof BowItem;
  }

  public static float applyBowDamageBonus(float damage) {
    return damage * 2.0F;
  }

  public static void beginBowUse(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VAGABOND)) {
      return;
    }
    SLOW_ARROWS_BEFORE_SHOT.put(player.getUUID(), countSlowArrows(player));
  }

  public static void tagSlowArrowIfNeeded(ServerPlayer shooter, AbstractArrow arrow) {
    if (!ChimereBlessingService.hasBlessing(shooter, GodIds.VAGABOND)) {
      return;
    }
    int before = SLOW_ARROWS_BEFORE_SHOT.getOrDefault(shooter.getUUID(), countSlowArrows(shooter));
    int after = countSlowArrows(shooter);
    if (after < before) {
      arrow.getPersistentData().putBoolean(FlecheLenteurHelper.SLOW_ARROW_PROJECTILE_TAG, true);
    }
    SLOW_ARROWS_BEFORE_SHOT.put(shooter.getUUID(), after);
  }

  public static void applySlowArrowHit(LivingEntity target, AbstractArrow arrow) {
    if (arrow.getPersistentData().getBoolean(FlecheLenteurHelper.SLOW_ARROW_PROJECTILE_TAG)) {
      target.addEffect(new MobEffectInstance(
          MobEffects.MOVEMENT_SLOWDOWN,
          SLOWNESS_DURATION_TICKS,
          0,
          false,
          true,
          true
      ));
    }
  }

  public static void tagSlowArrowIfNeeded(AbstractArrow arrow) {
    if (arrow.getOwner() instanceof ServerPlayer shooter) {
      tagSlowArrowIfNeeded(shooter, arrow);
    }
  }

  private static int countSlowArrows(ServerPlayer player) {
    int count = 0;
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (FlecheLenteurHelper.isSlowArrow(stack)) {
        count += stack.getCount();
      }
    }
    ItemStack carried = player.containerMenu.getCarried();
    if (FlecheLenteurHelper.isSlowArrow(carried)) {
      count += carried.getCount();
    }
    return count;
  }

  public static boolean isProtectedFromStrays(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.VAGABOND);
  }

  public static boolean isStrayAggressor(DamageSource source) {
    if (source.getEntity() instanceof Stray) {
      return true;
    }
    if (source.getDirectEntity() instanceof AbstractArrow arrow) {
      return arrow.getOwner() instanceof Stray;
    }
    return false;
  }

  public static void clearStrayAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VAGABOND)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(STRAY_AGGRO_CLEAR_RADIUS);
    for (Stray stray : player.serverLevel().getEntitiesOfClass(Stray.class, area)) {
      if (stray.getTarget() == player) {
        stray.setTarget(null);
      }
      if (stray.getLastHurtByMob() == player) {
        stray.setLastHurtByMob(null);
      }
    }
  }

  public static void clear(Player player) {
    SLOW_ARROW_COOLDOWN_LAST_TICK.remove(player.getUUID());
    SLOW_ARROWS_BEFORE_SHOT.remove(player.getUUID());
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
      FlecheLenteurHelper.purgeAllSlowArrows(serverPlayer);
    }
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

  private static void refreshNightVision(ServerPlayer player) {
    if (player.level().isNight()) {
      if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
        setNightVision(player, true);
      }
      return;
    }
    player.removeEffect(MobEffects.NIGHT_VISION);
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
