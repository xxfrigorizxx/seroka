package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Abeille : vol de faction, essaim et piqûre dévastatrice. */
public final class AbeilleBlessingHandler {

  private static final ResourceLocation FLYING_SPEED_ID = ModMain.id("abeille_flying_speed");
  private static final double FLYING_SPEED_MULTIPLIER = -0.7D;
  private static final float FACTION_FLIGHT_MIN_HEALTH_RATIO = 0.95F;
  private static final double BEE_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final double STING_RANGE = 16.0D;
  private static final float STING_PERCENT_DAMAGE_RATIO = 0.9F;
  private static final float STING_PIERCE_DAMAGE = 4.0F;
  private static final int BEE_SUMMON_COUNT = 10;
  private static final int BEE_SUMMON_COOLDOWN_TICKS = 10 * 60 * 20;
  private static final double BEE_FOLLOW_DISTANCE_SQR = 8.0D * 8.0D;
  private static final long DAY_LENGTH_TICKS = 24_000L;
  private static final Map<UUID, Long> BEE_SUMMON_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> STING_LAST_DAY = new ConcurrentHashMap<>();
  private static final Map<UUID, List<UUID>> SUMMONED_BEES = new ConcurrentHashMap<>();

  private AbeilleBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ABEILLE)) {
      return;
    }
    clearBeeAggro(player);
    refreshFactionFlight(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      refreshFactionFlight(player);
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ABEILLE)) {
      return;
    }
    clearBeeAggro(player);
    tickSummonedBees(player);
  }

  public static boolean hasOnlineBeeGod(MinecraftServer server) {
    for (ServerPlayer online : server.getPlayerList().getPlayers()) {
      if (ChimereBlessingService.hasBlessing(online, GodIds.ABEILLE)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isProtectedFromBees(LivingEntity entity) {
    if (!(entity instanceof Player player)) {
      return false;
    }
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return false;
    }
    return hasOnlineBeeGod(player.getServer());
  }

  public static boolean isBeeDamage(DamageSource source) {
    if (source.getEntity() instanceof Bee) {
      return true;
    }
    return source.getDirectEntity() instanceof Bee;
  }

  public static void clearBeeAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ABEILLE)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(BEE_AGGRO_CLEAR_RADIUS);
    for (Bee bee : player.serverLevel().getEntitiesOfClass(Bee.class, area)) {
      if (isProtectedFromBees(player) && bee.getTarget() == player) {
        bee.setTarget(null);
      }
      if (bee.getLastHurtByMob() == player) {
        bee.setLastHurtByMob(null);
      }
    }
  }

  public static void activateSpawnBees(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ABEILLE)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - BEE_SUMMON_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < BEE_SUMMON_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((BEE_SUMMON_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.abeille.summon.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    despawnSummons(player);
    ServerLevel level = player.serverLevel();
    List<UUID> summoned = new ArrayList<>();

    for (int index = 0; index < BEE_SUMMON_COUNT; index++) {
      Bee bee = EntityType.BEE.create(level);
      if (bee == null) {
        continue;
      }

      double angle = (Math.PI * 2.0D * index) / BEE_SUMMON_COUNT;
      double spawnX = player.getX() + Math.cos(angle) * 2.0D;
      double spawnZ = player.getZ() + Math.sin(angle) * 2.0D;
      bee.moveTo(spawnX, player.getY(), spawnZ, player.getYRot(), 0.0F);
      bee.finalizeSpawn(level, level.getCurrentDifficultyAt(bee.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
      bee.setPersistenceRequired();
      bee.setTarget(null);
      bee.setRemainingPersistentAngerTime(0);
      level.addFreshEntity(bee);
      summoned.add(bee.getUUID());
    }

    if (summoned.isEmpty()) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.abeille.summon.failed").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    SUMMONED_BEES.put(player.getUUID(), summoned);
    BEE_SUMMON_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.BEEHIVE_EXIT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.abeille.summon.used", BEE_SUMMON_COUNT)
            .withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static void activateSting(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ABEILLE)) {
      return;
    }

    long currentDay = player.level().getDayTime() / DAY_LENGTH_TICKS;
    Long lastDay = STING_LAST_DAY.get(player.getUUID());
    if (lastDay != null && lastDay == currentDay) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.abeille.sting.cooldown").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    LivingEntity target = findStingTarget(player);
    if (target == null) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.abeille.sting.no_target").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    applyStingDamage(player, target);
    STING_LAST_DAY.put(player.getUUID(), currentDay);
    player.playNotifySound(SoundEvents.BEE_STING, SoundSource.PLAYERS, 1.0F, 0.8F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.abeille.sting.used", target.getDisplayName())
            .withStyle(ChatFormatting.YELLOW),
        true
    );
  }

  public static void clear(ServerPlayer player) {
    BEE_SUMMON_COOLDOWN_LAST_TICK.remove(player.getUUID());
    STING_LAST_DAY.remove(player.getUUID());
    despawnSummons(player);
    removeFactionFlight(player);
  }

  public static void refreshFactionFlight(ServerPlayer player) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      removeFactionFlight(player);
      return;
    }
    if (!hasOnlineBeeGod(player.getServer())) {
      removeFactionFlight(player);
      return;
    }
    if (!canUseFactionFlight(player)) {
      disableFlight(player);
      return;
    }
    applyFlyingSpeedModifier(player);
    enableFlight(player);
  }

  public static boolean isFactionFlightAvailable(ServerPlayer player) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return false;
    }
    if (!hasOnlineBeeGod(player.getServer())) {
      return false;
    }
    return canUseFactionFlight(player);
  }

  private static boolean canUseFactionFlight(ServerPlayer player) {
    if (player.isCreative() || player.isSpectator()) {
      return false;
    }
    if (player.getMaxHealth() <= 0.0F) {
      return false;
    }
    return player.getHealth() > player.getMaxHealth() * FACTION_FLIGHT_MIN_HEALTH_RATIO;
  }

  private static void applyStingDamage(ServerPlayer player, LivingEntity target) {
    float percentDamage = target.getHealth() * STING_PERCENT_DAMAGE_RATIO;
    if (percentDamage > 0.0F) {
      target.hurt(player.damageSources().indirectMagic(player, player), percentDamage);
    }
    if (target.isAlive()) {
      target.hurt(player.damageSources().sonicBoom(player), STING_PIERCE_DAMAGE);
    }
  }

  private static LivingEntity findStingTarget(ServerPlayer player) {
    Vec3 eye = player.getEyePosition(1.0F);
    Vec3 look = player.getViewVector(1.0F);
    Vec3 end = eye.add(look.scale(STING_RANGE));
    AABB searchBox = player.getBoundingBox().expandTowards(look.scale(STING_RANGE)).inflate(1.0D);
    EntityHitResult hit = ProjectileUtil.getEntityHitResult(
        player,
        eye,
        end,
        searchBox,
        entity -> entity instanceof LivingEntity living
            && living != player
            && living.isAlive()
            && !living.isSpectator()
            && entity.isPickable(),
        STING_RANGE * STING_RANGE
    );
    if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
      return null;
    }
    return target;
  }

  private static void tickSummonedBees(ServerPlayer player) {
    List<UUID> summoned = SUMMONED_BEES.get(player.getUUID());
    if (summoned == null || summoned.isEmpty()) {
      return;
    }

    Iterator<UUID> iterator = summoned.iterator();
    while (iterator.hasNext()) {
      UUID entityId = iterator.next();
      Entity entity = player.serverLevel().getEntity(entityId);
      if (!(entity instanceof Bee bee) || !bee.isAlive()) {
        iterator.remove();
        continue;
      }

      if (bee.getTarget() == player) {
        bee.setTarget(null);
      }

      double distanceSqr = bee.distanceToSqr(player);
      if (distanceSqr > BEE_FOLLOW_DISTANCE_SQR) {
        bee.getNavigation().moveTo(player, 1.0D);
      } else if (bee.getNavigation().isDone()) {
        bee.getNavigation().stop();
      }
    }

    if (summoned.isEmpty()) {
      SUMMONED_BEES.remove(player.getUUID());
    }
  }

  private static void despawnSummons(ServerPlayer player) {
    List<UUID> summoned = SUMMONED_BEES.remove(player.getUUID());
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

  private static void applyFlyingSpeedModifier(ServerPlayer player) {
    AttributeInstance instance = player.getAttribute(Attributes.FLYING_SPEED);
    if (instance == null) {
      return;
    }
    if (instance.getModifier(FLYING_SPEED_ID) == null) {
      instance.addPermanentModifier(
          new AttributeModifier(FLYING_SPEED_ID, FLYING_SPEED_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
      );
    }
  }

  private static void removeFactionFlight(Player player) {
    removeFlyingSpeedModifier(player);
    if (player instanceof ServerPlayer serverPlayer) {
      disableFlight(serverPlayer);
    }
  }

  private static void removeFlyingSpeedModifier(Player player) {
    AttributeInstance instance = player.getAttribute(Attributes.FLYING_SPEED);
    if (instance != null) {
      instance.removeModifier(FLYING_SPEED_ID);
    }
  }

  private static void enableFlight(ServerPlayer player) {
    ChimereFlightHelper.grantMayfly(player);
  }

  private static void disableFlight(ServerPlayer player) {
    ChimereFlightHelper.revokeMayfly(player);
  }
}
