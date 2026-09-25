package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Golem de Fer : endurance, force et coup propulsif. */
public final class GolemFerBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("golem_fer_health");
  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("golem_fer_attack_damage");
  private static final double EXTRA_HEALTH = 20.0D;
  private static final double ATTACK_DAMAGE_BONUS = 0.1D;
  private static final float INCOMING_DAMAGE_MULTIPLIER = 0.75F;
  private static final double GOLEM_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final double SLAM_RANGE = 4.0D;
  private static final float SLAM_DAMAGE = 15.0F;
  private static final double SLAM_UPWARD = 1.1D;
  private static final double SLAM_KNOCKBACK = 1.4D;
  private static final int SLAM_COOLDOWN_TICKS = 30 * 20;
  private static final Map<UUID, Long> SLAM_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private GolemFerBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GOLEM_FER)) {
      return;
    }
    applyHealthBonus(player, true);
    applyAttackDamageBonus(player);
    clearIronGolemAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GOLEM_FER)) {
      return;
    }
    applyHealthBonus(player, false);
    applyAttackDamageBonus(player);
    clearIronGolemAggro(player);
  }

  public static float reduceIncomingDamage(ServerPlayer player, float damage) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GOLEM_FER)) {
      return damage;
    }
    return damage * INCOMING_DAMAGE_MULTIPLIER;
  }

  public static boolean isProtectedFromIronGolems(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.GOLEM_FER);
  }

  public static boolean isIronGolemDamage(DamageSource source) {
    return source.getEntity() instanceof IronGolem || source.getDirectEntity() instanceof IronGolem;
  }

  public static void activateSlam(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GOLEM_FER)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - SLAM_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < SLAM_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((SLAM_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.golem_fer.slam.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    LivingEntity target = findSlamTarget(player);
    if (target == null) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.golem_fer.slam.no_target").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    deliverSlam(player, target);
    SLAM_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.swing(InteractionHand.MAIN_HAND, true);
    player.playNotifySound(SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.golem_fer.slam.used", target.getDisplayName())
            .withStyle(ChatFormatting.GRAY),
        true
    );
  }

  public static void clear(Player player) {
    SLAM_COOLDOWN_LAST_TICK.remove(player.getUUID());
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
  }

  private static void deliverSlam(ServerPlayer player, LivingEntity target) {
    target.hurt(player.damageSources().playerAttack(player), SLAM_DAMAGE);
    double dx = target.getX() - player.getX();
    double dz = target.getZ() - player.getZ();
    target.knockback(SLAM_KNOCKBACK, dx, dz);
    Vec3 motion = target.getDeltaMovement();
    target.setDeltaMovement(motion.x, Math.max(motion.y, SLAM_UPWARD), motion.z);
    target.hurtMarked = true;
  }

  private static LivingEntity findSlamTarget(ServerPlayer player) {
    Vec3 eye = player.getEyePosition(1.0F);
    Vec3 look = player.getViewVector(1.0F);
    Vec3 end = eye.add(look.scale(SLAM_RANGE));
    AABB searchBox = player.getBoundingBox().expandTowards(look.scale(SLAM_RANGE)).inflate(1.0D);
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
        SLAM_RANGE * SLAM_RANGE
    );
    if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
      return null;
    }
    return target;
  }

  private static void clearIronGolemAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.GOLEM_FER)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(GOLEM_AGGRO_CLEAR_RADIUS);
    for (IronGolem golem : player.serverLevel().getEntitiesOfClass(IronGolem.class, area)) {
      if (golem.getTarget() == player) {
        golem.setTarget(null);
      }
      if (golem.getLastHurtByMob() == player) {
        golem.setLastHurtByMob(null);
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
        new AttributeModifier(HEALTH_ID, EXTRA_HEALTH, AttributeModifier.Operation.ADD_VALUE)
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
}
