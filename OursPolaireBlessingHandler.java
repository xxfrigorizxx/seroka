package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Ours Polaire : double vie, double dégâts et coup puissant. */
public final class OursPolaireBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("ours_polaire_health");
  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("ours_polaire_attack_damage");
  private static final double HEALTH_BONUS = 1.0D;
  private static final double ATTACK_DAMAGE_BONUS = 1.0D;
  private static final double SMASH_DAMAGE_MULTIPLIER = 3.0D;
  private static final int SMASH_COOLDOWN_TICKS = 20 * 60 * 20;
  private static final Map<UUID, Long> SMASH_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private OursPolaireBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.OURS_POLAIRE)) {
      return;
    }
    applyHealthBonus(player, true);
    applyAttackDamageBonus(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.OURS_POLAIRE)) {
      return;
    }
    applyHealthBonus(player, false);
    applyAttackDamageBonus(player);
  }

  public static void activateSmash(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.OURS_POLAIRE)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - SMASH_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < SMASH_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((SMASH_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.ours_polaire.smash.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    LivingEntity target = findMeleeTarget(player);
    if (target == null) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.ours_polaire.smash.no_target").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * SMASH_DAMAGE_MULTIPLIER);
    target.hurt(player.damageSources().playerAttack(player), damage);
    player.swing(InteractionHand.MAIN_HAND, true);
    SMASH_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.POLAR_BEAR_WARNING, SoundSource.PLAYERS, 1.0F, 0.8F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.ours_polaire.smash.hit", target.getDisplayName())
            .withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static void clear(Player player) {
    SMASH_COOLDOWN_LAST_TICK.remove(player.getUUID());
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

  private static LivingEntity findMeleeTarget(ServerPlayer player) {
    double range = player.entityInteractionRange();
    Vec3 eye = player.getEyePosition(1.0F);
    Vec3 look = player.getViewVector(1.0F);
    Vec3 end = eye.add(look.scale(range));
    AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
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
        range * range
    );
    if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
      return null;
    }
    return target;
  }
}
