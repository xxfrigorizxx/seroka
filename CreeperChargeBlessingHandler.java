package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Creeper Chargé : vision, grosse explosion et bonus légers. */
public final class CreeperChargeBlessingHandler {

  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("creeper_charge_attack_damage");
  private static final ResourceLocation SPEED_ID = ModMain.id("creeper_charge_speed");
  private static final double ATTACK_DAMAGE_BONUS = 0.05D;
  private static final double SPEED_BONUS = 0.05D;
  private static final int EXPLOSION_COOLDOWN_TICKS = 10 * 60 * 20;
  private static final int FUSE_TICKS = 30;
  private static final float EXPLOSION_RADIUS = 6.0F;
  private static final double CREEPER_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final Map<UUID, Long> EXPLOSION_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, PendingExplosion> PENDING_EXPLOSIONS = new ConcurrentHashMap<>();
  private static final Set<UUID> EXPLOSION_IMMUNE_PLAYERS = ConcurrentHashMap.newKeySet();

  private CreeperChargeBlessingHandler() {}

  private record PendingExplosion(long explodeAtTick, long immunityStartTick) {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CREEPER_CHARGE)) {
      return;
    }
    setNightVision(player, true);
    applyAttackDamageBonus(player);
    applySpeedBonus(player);
    clearCreeperAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CREEPER_CHARGE)) {
      return;
    }
    if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
      setNightVision(player, true);
    }
    applyAttackDamageBonus(player);
    applySpeedBonus(player);
    tickPendingExplosion(player);
  }

  public static void activateExplosion(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CREEPER_CHARGE)) {
      return;
    }
    if (PENDING_EXPLOSIONS.containsKey(player.getUUID())) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.creeper_charge.explosion.charging").withStyle(ChatFormatting.YELLOW),
          true
      );
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - EXPLOSION_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < EXPLOSION_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((EXPLOSION_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.creeper_charge.explosion.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    PENDING_EXPLOSIONS.put(player.getUUID(), new PendingExplosion(now + FUSE_TICKS, now + FUSE_TICKS - 1));
    EXPLOSION_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.CREEPER_PRIMED, SoundSource.PLAYERS, 1.0F, 0.8F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.creeper_charge.explosion.primed").withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static boolean hasExplosionImmunity(ServerPlayer player) {
    return EXPLOSION_IMMUNE_PLAYERS.contains(player.getUUID());
  }

  public static boolean isProtectedFromCreepers(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.CREEPER_CHARGE);
  }

  public static void clearCreeperAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CREEPER_CHARGE)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(CREEPER_AGGRO_CLEAR_RADIUS);
    for (Creeper creeper : player.serverLevel().getEntitiesOfClass(Creeper.class, area)) {
      if (creeper.getTarget() == player) {
        creeper.setTarget(null);
      }
      if (creeper.getLastHurtByMob() == player) {
        creeper.setLastHurtByMob(null);
      }
    }
  }

  public static void cancelPending(ServerPlayer player) {
    PENDING_EXPLOSIONS.remove(player.getUUID());
    EXPLOSION_IMMUNE_PLAYERS.remove(player.getUUID());
  }

  public static void clear(Player player) {
    EXPLOSION_COOLDOWN_LAST_TICK.remove(player.getUUID());
    PENDING_EXPLOSIONS.remove(player.getUUID());
    EXPLOSION_IMMUNE_PLAYERS.remove(player.getUUID());
    AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attack != null) {
      attack.removeModifier(ATTACK_DAMAGE_ID);
    }
    AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (speed != null) {
      speed.removeModifier(SPEED_ID);
    }
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
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

  private static void applySpeedBonus(Player player) {
    AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (instance == null) {
      return;
    }
    instance.removeModifier(SPEED_ID);
    instance.addPermanentModifier(
        new AttributeModifier(SPEED_ID, SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );
  }

  private static void tickPendingExplosion(ServerPlayer player) {
    PendingExplosion pending = PENDING_EXPLOSIONS.get(player.getUUID());
    if (pending == null) {
      return;
    }

    long now = player.level().getGameTime();
    if (now >= pending.immunityStartTick()) {
      EXPLOSION_IMMUNE_PLAYERS.add(player.getUUID());
    }
    if (now < pending.explodeAtTick()) {
      return;
    }

    triggerExplosion(player);
    PENDING_EXPLOSIONS.remove(player.getUUID());
    EXPLOSION_IMMUNE_PLAYERS.remove(player.getUUID());
  }

  private static void triggerExplosion(ServerPlayer player) {
    player.serverLevel().explode(
        player,
        player.getX(),
        player.getY(),
        player.getZ(),
        EXPLOSION_RADIUS,
        Level.ExplosionInteraction.MOB
    );
    player.displayClientMessage(
        Component.translatable("blessing.seroka.creeper_charge.explosion.detonated").withStyle(ChatFormatting.GRAY),
        true
    );
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
