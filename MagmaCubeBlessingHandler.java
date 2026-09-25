package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Magma Cube : résistances, force et saut rebondissant. */
public final class MagmaCubeBlessingHandler {

  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("magma_cube_attack_damage");
  private static final double ATTACK_DAMAGE_BONUS = 0.25D;
  private static final double JUMP_VELOCITY_Y = 0.70D;
  private static final int JUMP_COOLDOWN_TICKS = 20 * 20;
  private static final Map<UUID, Long> JUMP_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private MagmaCubeBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.MAGMA_CUBE)) {
      return;
    }
    applyAttackDamageBonus(player);
    extinguishIfBurning(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.MAGMA_CUBE)) {
      return;
    }
    applyAttackDamageBonus(player);
    extinguishIfBurning(player);
  }

  public static void activateJump(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.MAGMA_CUBE)) {
      return;
    }
    if (!player.onGround() && !player.isInWater()) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.magma_cube.jump.grounded").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - JUMP_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < JUMP_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((JUMP_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.magma_cube.jump.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    JUMP_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    Vec3 motion = player.getDeltaMovement();
    player.setDeltaMovement(motion.x, JUMP_VELOCITY_Y, motion.z);
    player.fallDistance = 0.0F;
    player.hurtMarked = true;
    player.playNotifySound(SoundEvents.MAGMA_CUBE_SQUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.magma_cube.jump.used").withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static boolean isFireLavaOrFallDamage(DamageSource source) {
    return source.is(DamageTypeTags.IS_FIRE)
        || source.is(DamageTypes.LAVA)
        || source.is(DamageTypes.FALL);
  }

  public static void clear(Player player) {
    AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attack != null) {
      attack.removeModifier(ATTACK_DAMAGE_ID);
    }
    JUMP_COOLDOWN_LAST_TICK.remove(player.getUUID());
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

  private static void extinguishIfBurning(ServerPlayer player) {
    if (player.isOnFire()) {
      player.clearFire();
    }
  }
}
