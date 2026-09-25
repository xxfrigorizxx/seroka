package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Évocateur : totem, mâchoires, vex et rituel du mouton. */
public final class EvocateurBlessingHandler {

  private static final String OWNER_TAG = ModMain.MODID + ":evocateur_owner";
  private static final int LINE_FANG_COUNT = 12;
  private static final double LINE_FANG_SPACING = 1.0D;
  private static final int CIRCLE_FANG_COUNT = 8;
  private static final double CIRCLE_FANG_RADIUS = 3.0D;
  private static final int VEX_SUMMON_COUNT = 2;
  private static final double VEX_FOLLOW_DISTANCE_SQR = 8.0D * 8.0D;
  private static final int LINE_FANG_COOLDOWN_TICKS = 30 * 20;
  private static final int CIRCLE_FANG_COOLDOWN_TICKS = 45 * 20;
  private static final int VEX_SUMMON_COOLDOWN_TICKS = 5 * 60 * 20;
  private static final double SHEEP_INTERACT_RANGE = 6.0D;

  private static final Map<UUID, Long> LINE_FANG_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> CIRCLE_FANG_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> VEX_SUMMON_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Boolean> TOTEM_AVAILABLE = new ConcurrentHashMap<>();
  private static final Map<UUID, List<UUID>> SUMMONED_VEX = new ConcurrentHashMap<>();

  private EvocateurBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EVOCATEUR)) {
      return;
    }
    resetTotemForLife(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EVOCATEUR)) {
      return;
    }
    tickSummonedVex(player);
  }

  public static void resetTotemForLife(ServerPlayer player) {
    TOTEM_AVAILABLE.put(player.getUUID(), true);
  }

  public static boolean tryUseTotem(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EVOCATEUR)) {
      return false;
    }
    if (!TOTEM_AVAILABLE.getOrDefault(player.getUUID(), false)) {
      return false;
    }
    TOTEM_AVAILABLE.put(player.getUUID(), false);
    player.setHealth(1.0F);
    player.removeAllEffects();
    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
    player.playNotifySound(SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
    if (player.serverLevel() instanceof ServerLevel serverLevel) {
      serverLevel.sendParticles(
          ParticleTypes.TOTEM_OF_UNDYING,
          player.getX(),
          player.getY() + player.getBbHeight() / 2.0D,
          player.getZ(),
          30,
          0.25D,
          0.5D,
          0.25D,
          0.15D
      );
    }
    player.displayClientMessage(
        Component.translatable("blessing.seroka.evocateur.totem.used").withStyle(ChatFormatting.GOLD),
        true
    );
    return true;
  }

  public static void activateLineFangs(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EVOCATEUR)) {
      return;
    }

    Vec3 look = player.getLookAngle();
    Vec3 horizontal = new Vec3(look.x, 0.0D, look.z).normalize();
    if (horizontal.lengthSqr() < 1.0E-4D) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.evocateur.fangs.no_direction").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    if (!tryConsumeCooldown(player, LINE_FANG_COOLDOWN_LAST_TICK, LINE_FANG_COOLDOWN_TICKS, "blessing.seroka.evocateur.fangs.line.cooldown")) {
      return;
    }

    spawnLineFangs(player, horizontal);
    player.playNotifySound(SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.evocateur.fangs.line.used").withStyle(ChatFormatting.DARK_PURPLE),
        true
    );
  }

  public static void activateCircleFangs(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EVOCATEUR)) {
      return;
    }
    if (!tryConsumeCooldown(player, CIRCLE_FANG_COOLDOWN_LAST_TICK, CIRCLE_FANG_COOLDOWN_TICKS, "blessing.seroka.evocateur.fangs.circle.cooldown")) {
      return;
    }

    spawnCircleFangs(player);
    player.playNotifySound(SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.evocateur.fangs.circle.used").withStyle(ChatFormatting.DARK_PURPLE),
        true
    );
  }

  public static void activateVexSummon(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EVOCATEUR)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - VEX_SUMMON_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < VEX_SUMMON_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((VEX_SUMMON_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.evocateur.vex.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    despawnSummonedVex(player);
    ServerLevel level = player.serverLevel();
    List<UUID> summoned = new ArrayList<>();

    for (int index = 0; index < VEX_SUMMON_COUNT; index++) {
      Vex vex = EntityType.VEX.create(level);
      if (vex == null) {
        continue;
      }

      Vec3 offset = Vec3.directionFromRotation(0.0F, player.getYRot() + (index == 0 ? 90.0F : -90.0F)).scale(1.5D);
      vex.moveTo(player.getX() + offset.x, player.getY(), player.getZ() + offset.z, player.getYRot(), 0.0F);
      vex.finalizeSpawn(level, level.getCurrentDifficultyAt(vex.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
      vex.setPersistenceRequired();
      vex.setBoundOrigin(player.blockPosition());
      vex.getPersistentData().putUUID(OWNER_TAG, player.getUUID());
      vex.setTarget(null);
      level.addFreshEntity(vex);
      summoned.add(vex.getUUID());
    }

    if (summoned.isEmpty()) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.evocateur.vex.failed").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    SUMMONED_VEX.put(player.getUUID(), summoned);
    VEX_SUMMON_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.evocateur.vex.used", VEX_SUMMON_COUNT)
            .withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static boolean tryDyeBlueSheep(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EVOCATEUR)) {
      return false;
    }
    if (!player.isShiftKeyDown()) {
      return false;
    }

    HitResult hit = player.pick(SHEEP_INTERACT_RANGE, 0.0F, false);
    if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof Sheep sheep)) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.evocateur.sheep.no_target").withStyle(ChatFormatting.RED),
          true
      );
      return true;
    }
    if (sheep.getColor() != DyeColor.BLUE) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.evocateur.sheep.not_blue").withStyle(ChatFormatting.RED),
          true
      );
      return true;
    }

    sheep.setColor(DyeColor.RED);
    player.playNotifySound(SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.2F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.evocateur.sheep.dyed").withStyle(ChatFormatting.LIGHT_PURPLE),
        true
    );
    return true;
  }

  public static void onOwnerAttack(ServerPlayer player, LivingEntity target) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.EVOCATEUR)) {
      return;
    }
    List<UUID> vexIds = SUMMONED_VEX.get(player.getUUID());
    if (vexIds == null) {
      return;
    }
    for (UUID vexId : vexIds) {
      Entity entity = player.serverLevel().getEntity(vexId);
      if (entity instanceof Vex vex && vex.isAlive() && isOwnedVex(vex, player)) {
        vex.setTarget(target);
      }
    }
  }

  public static void clear(ServerPlayer player) {
    LINE_FANG_COOLDOWN_LAST_TICK.remove(player.getUUID());
    CIRCLE_FANG_COOLDOWN_LAST_TICK.remove(player.getUUID());
    VEX_SUMMON_COOLDOWN_LAST_TICK.remove(player.getUUID());
    TOTEM_AVAILABLE.remove(player.getUUID());
    despawnSummonedVex(player);
  }

  private static void spawnLineFangs(ServerPlayer player, Vec3 direction) {
    ServerLevel level = player.serverLevel();
    Vec3 start = player.position().add(0.0D, 0.05D, 0.0D);
    float yaw = player.getYRot();

    for (int index = 0; index < LINE_FANG_COUNT; index++) {
      Vec3 point = start.add(direction.scale((index + 1) * LINE_FANG_SPACING));
      double y = findFangY(level, point.x, point.z, point.y);
      EvokerFangs fangs = new EvokerFangs(level, point.x, y, point.z, yaw, index * 2, player);
      level.addFreshEntity(fangs);
    }
  }

  private static void spawnCircleFangs(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    double centerX = player.getX();
    double centerZ = player.getZ();
    float yaw = player.getYRot();

    for (int index = 0; index < CIRCLE_FANG_COUNT; index++) {
      double angle = (Math.PI * 2.0D * index) / CIRCLE_FANG_COUNT;
      double x = centerX + Math.cos(angle) * CIRCLE_FANG_RADIUS;
      double z = centerZ + Math.sin(angle) * CIRCLE_FANG_RADIUS;
      double y = findFangY(level, x, z, player.getY());
      EvokerFangs fangs = new EvokerFangs(level, x, y, z, yaw, index * 2, player);
      level.addFreshEntity(fangs);
    }
  }

  private static double findFangY(Level level, double x, double z, double fallbackY) {
    BlockPos start = BlockPos.containing(x, fallbackY + 1.0D, z);
    for (int offset = 0; offset < 6; offset++) {
      BlockPos below = start.below(offset);
      if (level.getBlockState(below).blocksMotion()) {
        return below.getY() + 1.0D;
      }
    }
    return fallbackY;
  }

  private static boolean tryConsumeCooldown(
      ServerPlayer player,
      Map<UUID, Long> cooldownMap,
      int cooldownTicks,
      String messageKey
  ) {
    long now = player.level().getGameTime();
    long elapsed = now - cooldownMap.getOrDefault(player.getUUID(), 0L);
    if (elapsed < cooldownTicks) {
      int remainingSeconds = (int) Math.ceil((cooldownTicks - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable(messageKey, remainingSeconds).withStyle(ChatFormatting.RED),
          true
      );
      return false;
    }
    cooldownMap.put(player.getUUID(), now);
    return true;
  }

  private static void tickSummonedVex(ServerPlayer player) {
    List<UUID> vexIds = SUMMONED_VEX.get(player.getUUID());
    if (vexIds == null || vexIds.isEmpty()) {
      return;
    }

    Iterator<UUID> iterator = vexIds.iterator();
    while (iterator.hasNext()) {
      UUID vexId = iterator.next();
      Entity entity = player.serverLevel().getEntity(vexId);
      if (!(entity instanceof Vex vex) || !vex.isAlive() || !isOwnedVex(vex, player)) {
        iterator.remove();
        continue;
      }

      if (vex.getTarget() == player) {
        vex.setTarget(null);
      }

      if (vex.distanceToSqr(player) > VEX_FOLLOW_DISTANCE_SQR) {
        vex.getNavigation().moveTo(player, 1.1D);
      } else if (vex.getTarget() == null) {
        vex.getNavigation().stop();
      }
    }

    if (vexIds.isEmpty()) {
      SUMMONED_VEX.remove(player.getUUID());
    }
  }

  private static boolean isOwnedVex(Vex vex, ServerPlayer player) {
    return vex.getPersistentData().hasUUID(OWNER_TAG)
        && vex.getPersistentData().getUUID(OWNER_TAG).equals(player.getUUID());
  }

  private static void despawnSummonedVex(ServerPlayer player) {
    List<UUID> vexIds = SUMMONED_VEX.remove(player.getUUID());
    if (vexIds == null) {
      return;
    }
    for (UUID vexId : vexIds) {
      Entity entity = player.serverLevel().getEntity(vexId);
      if (entity != null) {
        entity.discard();
      }
    }
  }
}
