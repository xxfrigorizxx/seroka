package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Grand Gardien : endurance, épines II, rayon et malédiction. */
public final class GrandGardienBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("grand_gardien_health");
  private static final double HEALTH_BONUS = 2.0D;
  private static final float INCOMING_DAMAGE_MULTIPLIER = 0.5F;
  private static final double BEAM_RANGE = 16.0D;
  private static final float BEAM_DAMAGE = 9.0F;
  private static final int BEAM_COOLDOWN_TICKS = 25 * 20;
  private static final int MINING_FATIGUE_RADIUS = 50;
  private static final int MINING_FATIGUE_DURATION_TICKS = 5 * 60 * 20;
  private static final int MINING_FATIGUE_AMPLIFIER = 2;
  private static final int MINING_FATIGUE_COOLDOWN_TICKS = 60 * 20;
  private static final Map<UUID, Long> BEAM_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> MINING_FATIGUE_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final ThreadLocal<Boolean> APPLYING_THORNS = ThreadLocal.withInitial(() -> false);

  private GrandGardienBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GRAND_GARDIEN)) {
      return;
    }
    applyHealthBonus(player, true);
    setWaterBreathing(player, true);
    if (player.isUnderWater()) {
      setUnderwaterNightVision(player, true);
    }
    GardienBlessingHandler.clearGuardianAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GRAND_GARDIEN)) {
      return;
    }
    applyHealthBonus(player, false);
    if (!player.hasEffect(MobEffects.WATER_BREATHING)) {
      setWaterBreathing(player, true);
    }
    if (player.isUnderWater()) {
      player.setAirSupply(player.getMaxAirSupply());
      if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
        setUnderwaterNightVision(player, true);
      }
    } else {
      player.removeEffect(MobEffects.NIGHT_VISION);
    }
    GardienBlessingHandler.clearGuardianAggro(player);
  }

  public static float reduceIncomingDamage(ServerPlayer player, float damage) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GRAND_GARDIEN)) {
      return damage;
    }
    return damage * INCOMING_DAMAGE_MULTIPLIER;
  }

  public static void applyThornsReflect(ServerPlayer victim, LivingEntity attacker) {
    if (!ChimereBlessingService.hasBlessing(victim, GodIds.GRAND_GARDIEN)) {
      return;
    }
    if (APPLYING_THORNS.get()) {
      return;
    }
    float thornsDamage = 2.0F + victim.getRandom().nextInt(5);
    APPLYING_THORNS.set(true);
    try {
      attacker.hurt(victim.damageSources().thorns(victim), thornsDamage);
    } finally {
      APPLYING_THORNS.set(false);
    }
  }

  public static void activateBeam(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GRAND_GARDIEN)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - BEAM_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < BEAM_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((BEAM_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.grand_gardien.beam.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    LivingEntity target = findBeamTarget(player);
    if (target == null) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.grand_gardien.beam.no_target").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    BEAM_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    Vec3 from = player.getEyePosition(1.0F);
    Vec3 to = target.getEyePosition(1.0F);
    spawnBeamParticles(player.serverLevel(), from, to, player.isUnderWater());
    target.hurt(player.damageSources().indirectMagic(player, player), BEAM_DAMAGE);
    player.playNotifySound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.grand_gardien.beam.used", target.getDisplayName())
            .withStyle(ChatFormatting.DARK_AQUA),
        true
    );
  }

  public static void activateMiningFatigue(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GRAND_GARDIEN)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - MINING_FATIGUE_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < MINING_FATIGUE_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((MINING_FATIGUE_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.grand_gardien.curse.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    AABB area = player.getBoundingBox().inflate(MINING_FATIGUE_RADIUS);
    int affected = 0;
    for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area)) {
      if (entity == player || !entity.isAlive() || entity.isSpectator()) {
        continue;
      }
      entity.addEffect(new MobEffectInstance(
          MobEffects.DIG_SLOWDOWN,
          MINING_FATIGUE_DURATION_TICKS,
          MINING_FATIGUE_AMPLIFIER,
          false,
          true,
          true
      ));
      affected++;
    }

    if (affected == 0) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.grand_gardien.curse.no_target").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    MINING_FATIGUE_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.0F, 0.8F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.grand_gardien.curse.used", affected)
            .withStyle(ChatFormatting.DARK_PURPLE),
        true
    );
  }

  public static void clear(Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.WATER_BREATHING);
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
    }
    AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
    if (health != null) {
      health.removeModifier(HEALTH_ID);
    }
    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
    }
    BEAM_COOLDOWN_LAST_TICK.remove(player.getUUID());
    MINING_FATIGUE_COOLDOWN_LAST_TICK.remove(player.getUUID());
  }

  private static void applyHealthBonus(Player player, boolean fillToMax) {
    AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
    if (instance == null) {
      return;
    }

    float ratio = player.getMaxHealth() > 0.0F ? player.getHealth() / player.getMaxHealth() : 1.0F;
    instance.removeModifier(HEALTH_ID);
    instance.addPermanentModifier(
        new AttributeModifier(HEALTH_ID, HEALTH_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );

    if (fillToMax) {
      player.setHealth(player.getMaxHealth());
    } else {
      player.setHealth(Math.min(player.getMaxHealth(), player.getMaxHealth() * ratio));
    }
  }

  private static LivingEntity findBeamTarget(ServerPlayer player) {
    Vec3 eye = player.getEyePosition(1.0F);
    Vec3 look = player.getViewVector(1.0F);
    Vec3 end = eye.add(look.scale(BEAM_RANGE));
    AABB searchBox = player.getBoundingBox().expandTowards(look.scale(BEAM_RANGE)).inflate(1.0D);
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
        BEAM_RANGE * BEAM_RANGE
    );
    if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
      return null;
    }
    return target;
  }

  private static void spawnBeamParticles(ServerLevel level, Vec3 from, Vec3 to, boolean underwater) {
    Vec3 delta = to.subtract(from);
    double length = delta.length();
    if (length < 1.0E-4D) {
      return;
    }
    Vec3 step = delta.normalize().scale(0.35D);
    Vec3 pos = from;
    var particle = underwater ? ParticleTypes.BUBBLE_POP : ParticleTypes.END_ROD;
    for (double traveled = 0.0D; traveled < length; traveled += 0.35D) {
      level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
      pos = pos.add(step);
    }
  }

  private static void setUnderwaterNightVision(ServerPlayer player, boolean enabled) {
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

  private static void setWaterBreathing(ServerPlayer player, boolean enabled) {
    if (!enabled) {
      player.removeEffect(MobEffects.WATER_BREATHING);
      return;
    }
    player.addEffect(new MobEffectInstance(
        MobEffects.WATER_BREATHING,
        MobEffectInstance.INFINITE_DURATION,
        0,
        false,
        false,
        true
    ));
  }
}
