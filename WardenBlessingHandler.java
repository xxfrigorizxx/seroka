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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Warden : endurance, perforation et sonic boom. */
public final class WardenBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("warden_health");
  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("warden_attack_damage");
  private static final double HEALTH_BONUS = 5.0D;
  private static final double ATTACK_DAMAGE_BONUS = 1.0D;
  private static final double WARDEN_AGGRO_CLEAR_RADIUS = 48.0D;
  private static final double SONIC_BOOM_RANGE = 15.0D;
  private static final float SONIC_BOOM_DAMAGE = 10.0F;
  private static final int SONIC_BOOM_COOLDOWN_TICKS = 2 * 60 * 20;
  private static final Map<UUID, Long> SONIC_BOOM_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Float> PENDING_STRIKE_DAMAGE = new ConcurrentHashMap<>();
  private static final ThreadLocal<Boolean> APPLYING_ARMOR_PIERCE = ThreadLocal.withInitial(() -> false);

  private WardenBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.WARDEN)) {
      return;
    }
    applyHealthBonus(player, true);
    applyAttackDamageBonus(player);
    clearWardenAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.WARDEN)) {
      return;
    }
    applyHealthBonus(player, false);
    applyAttackDamageBonus(player);
    clearWardenAggro(player);
  }

  public static void recordStrikeDamage(ServerPlayer attacker, float damage) {
    if (!ChimereBlessingService.hasBlessing(attacker, GodIds.WARDEN)) {
      return;
    }
    if (damage > 0.0F) {
      PENDING_STRIKE_DAMAGE.put(attacker.getUUID(), damage);
    }
  }

  public static void applyArmorPierceFollowUp(ServerPlayer attacker, LivingEntity target, float dealtDamage) {
    if (!ChimereBlessingService.hasBlessing(attacker, GodIds.WARDEN)) {
      return;
    }
    if (APPLYING_ARMOR_PIERCE.get()) {
      return;
    }
    Float pending = PENDING_STRIKE_DAMAGE.remove(attacker.getUUID());
    if (pending == null) {
      return;
    }
    float blocked = pending - dealtDamage;
    if (blocked <= 0.0F) {
      return;
    }

    APPLYING_ARMOR_PIERCE.set(true);
    try {
      target.hurt(attacker.damageSources().sonicBoom(attacker), blocked);
    } finally {
      APPLYING_ARMOR_PIERCE.set(false);
    }
  }

  public static void activateSonicBoom(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.WARDEN)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - SONIC_BOOM_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < SONIC_BOOM_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((SONIC_BOOM_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.warden.sonic_boom.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    LivingEntity target = findSonicBoomTarget(player);
    if (target == null) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.warden.sonic_boom.no_target").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    SONIC_BOOM_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    Vec3 from = player.getEyePosition(1.0F);
    Vec3 to = target.getEyePosition(1.0F);
    spawnSonicBoomParticles(player.serverLevel(), from, to);
    target.hurt(player.damageSources().sonicBoom(player), SONIC_BOOM_DAMAGE);
    player.playNotifySound(SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.warden.sonic_boom.used", target.getDisplayName())
            .withStyle(ChatFormatting.DARK_PURPLE),
        true
    );
  }

  public static boolean isProtectedFromWardens(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.WARDEN);
  }

  public static boolean isWardenDamage(DamageSource source) {
    return source.getEntity() instanceof Warden || source.getDirectEntity() instanceof Warden;
  }

  public static void clear(Player player) {
    AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
    if (health != null) {
      health.removeModifier(HEALTH_ID);
    }
    AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attack != null) {
      attack.removeModifier(ATTACK_DAMAGE_ID);
    }
    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
    }
    SONIC_BOOM_COOLDOWN_LAST_TICK.remove(player.getUUID());
    PENDING_STRIKE_DAMAGE.remove(player.getUUID());
  }

  public static void clearWardenAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.WARDEN)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(WARDEN_AGGRO_CLEAR_RADIUS);
    for (Warden warden : player.serverLevel().getEntitiesOfClass(Warden.class, area)) {
      if (warden.getTarget() == player) {
        warden.setTarget(null);
      }
      if (warden.getLastHurtByMob() == player) {
        warden.setLastHurtByMob(null);
      }
    }
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

  private static void applyAttackDamageBonus(Player player) {
    AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (instance == null) {
      return;
    }
    instance.removeModifier(ATTACK_DAMAGE_ID);
    instance.addPermanentModifier(
        new AttributeModifier(ATTACK_DAMAGE_ID, ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );
  }

  private static LivingEntity findSonicBoomTarget(ServerPlayer player) {
    Vec3 eye = player.getEyePosition(1.0F);
    Vec3 look = player.getViewVector(1.0F);
    Vec3 end = eye.add(look.scale(SONIC_BOOM_RANGE));
    AABB searchBox = player.getBoundingBox().expandTowards(look.scale(SONIC_BOOM_RANGE)).inflate(1.0D);
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
        SONIC_BOOM_RANGE * SONIC_BOOM_RANGE
    );
    if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
      return null;
    }
    return target;
  }

  private static void spawnSonicBoomParticles(ServerLevel level, Vec3 from, Vec3 to) {
    Vec3 delta = to.subtract(from);
    double length = delta.length();
    if (length < 1.0E-4D) {
      return;
    }
    Vec3 step = delta.normalize().scale(0.5D);
    Vec3 pos = from;
    for (double traveled = 0.0D; traveled < length; traveled += 0.5D) {
      level.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
      pos = pos.add(step);
    }
  }
}
