package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Pillard : arbalète, flèches liées et pillards alliés. */
public final class PillardBlessingHandler {

  private static final int ARROW_COUNT = 10;
  private static final int ARROW_COOLDOWN_TICKS = 10 * 20;
  private static final int SUMMON_COOLDOWN_TICKS = 5 * 60 * 20;
  private static final int PILLAGER_SUMMON_COUNT = 2;
  private static final double PILLAGER_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final double FOLLOW_DISTANCE_SQR = 6.0D * 6.0D;
  private static final Map<UUID, Long> ARROW_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> SUMMON_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, List<UUID>> SUMMONED_PILLAGERS = new ConcurrentHashMap<>();

  private PillardBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PILLARD)) {
      return;
    }
    ensureCrossbow(player);
    clearPillagerAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PILLARD)) {
      return;
    }
    if (player.tickCount % 40 == 0) {
      ensureCrossbow(player);
    }
    FlechePillardHelper.purgeOutsidePlayerInventory(player);
    clearPillagerAggro(player);
    tickSummonedPillagers(player);
  }

  public static void activateArrows(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PILLARD)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - ARROW_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < ARROW_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((ARROW_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.pillard.arrows.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    ItemStack arrows = FlechePillardHelper.createStack(ARROW_COUNT);
    if (!player.getInventory().add(arrows)) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.pillard.arrows.inventory_full")
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    ARROW_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.8F, 0.9F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.pillard.arrows.granted", ARROW_COUNT)
            .withStyle(ChatFormatting.GREEN),
        true
    );
  }

  public static void activateSummon(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PILLARD)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - SUMMON_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < SUMMON_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((SUMMON_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.pillard.summon.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    despawnSummons(player);
    ServerLevel level = player.serverLevel();
    List<UUID> summoned = new ArrayList<>();

    for (int index = 0; index < PILLAGER_SUMMON_COUNT; index++) {
      Pillager pillager = EntityType.PILLAGER.create(level);
      if (pillager == null) {
        continue;
      }

      Vec3 offset = Vec3.directionFromRotation(0.0F, player.getYRot() + (index == 0 ? 90.0F : -90.0F)).scale(1.5D);
      double spawnX = player.getX() + offset.x;
      double spawnY = player.getY();
      double spawnZ = player.getZ() + offset.z;
      pillager.moveTo(spawnX, spawnY, spawnZ, player.getYRot(), 0.0F);
      pillager.finalizeSpawn(level, level.getCurrentDifficultyAt(pillager.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
      pillager.setPersistenceRequired();
      pillager.setCanPickUpLoot(false);
      pillager.setTarget(null);
      level.addFreshEntity(pillager);
      summoned.add(pillager.getUUID());
    }

    if (summoned.isEmpty()) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.pillard.summon.failed").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    SUMMONED_PILLAGERS.put(player.getUUID(), summoned);
    SUMMON_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.PILLAGER_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.pillard.summon.used", PILLAGER_SUMMON_COUNT)
            .withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static boolean isProtectedFromPillagers(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.PILLARD);
  }

  public static void clearPillagerAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PILLARD)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(PILLAGER_AGGRO_CLEAR_RADIUS);
    for (Pillager pillager : player.serverLevel().getEntitiesOfClass(Pillager.class, area)) {
      if (pillager.getTarget() == player) {
        pillager.setTarget(null);
      }
      if (pillager.getLastHurtByMob() == player) {
        pillager.setLastHurtByMob(null);
      }
    }
  }

  public static void clear(ServerPlayer player) {
    ARROW_COOLDOWN_LAST_TICK.remove(player.getUUID());
    SUMMON_COOLDOWN_LAST_TICK.remove(player.getUUID());
    despawnSummons(player);
    FlechePillardHelper.purgeAllPillardArrows(player);
  }

  private static void tickSummonedPillagers(ServerPlayer player) {
    List<UUID> summoned = SUMMONED_PILLAGERS.get(player.getUUID());
    if (summoned == null || summoned.isEmpty()) {
      return;
    }

    Iterator<UUID> iterator = summoned.iterator();
    while (iterator.hasNext()) {
      UUID entityId = iterator.next();
      Entity entity = player.serverLevel().getEntity(entityId);
      if (!(entity instanceof Pillager pillager) || !pillager.isAlive()) {
        iterator.remove();
        continue;
      }

      if (pillager.getTarget() == player) {
        pillager.setTarget(null);
      }

      double distanceSqr = pillager.distanceToSqr(player);
      if (distanceSqr > FOLLOW_DISTANCE_SQR) {
        pillager.getNavigation().moveTo(player, 1.1D);
      } else {
        pillager.getNavigation().stop();
      }
    }

    if (summoned.isEmpty()) {
      SUMMONED_PILLAGERS.remove(player.getUUID());
    }
  }

  private static void despawnSummons(ServerPlayer player) {
    List<UUID> summoned = SUMMONED_PILLAGERS.remove(player.getUUID());
    if (summoned == null) {
      return;
    }
    for (UUID entityId : summoned) {
      Entity entity = player.serverLevel().getEntity(entityId);
      if (entity != null) {
        entity.discard();
      }
    }
  }

  private static void ensureCrossbow(ServerPlayer player) {
    if (hasCrossbow(player)) {
      return;
    }
    ItemStack crossbow = new ItemStack(Items.CROSSBOW);
    if (!player.getInventory().add(crossbow)) {
      player.drop(crossbow, false);
    }
  }

  private static boolean hasCrossbow(Player player) {
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (!stack.isEmpty() && stack.getItem() instanceof CrossbowItem) {
        return true;
      }
    }
    return player.getMainHandItem().getItem() instanceof CrossbowItem
        || player.getOffhandItem().getItem() instanceof CrossbowItem;
  }
}
