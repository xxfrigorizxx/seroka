package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Ravageur : résistance, force et destruction. */
public final class RavageurBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("ravageur_health");
  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("ravageur_attack_damage");
  private static final double HEALTH_BONUS = 2.0D;
  private static final double ATTACK_DAMAGE_BONUS = 1.0D;
  private static final float INCOMING_DAMAGE_MULTIPLIER = 0.5F;
  private static final float RAMPAGE_PIERCE_MULTIPLIER = 2.0F;
  private static final int RAMPAGE_DURATION_TICKS = 5 * 20;
  private static final int RAMPAGE_COOLDOWN_TICKS = 60 * 20;
  private static final int CHARGE_COOLDOWN_TICKS = 5 * 60 * 20;
  private static final int CHARGE_WIDTH = 5;
  private static final int CHARGE_DEPTH = 4;

  private static final Map<UUID, Long> RAMPAGE_UNTIL_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> RAMPAGE_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> CHARGE_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final ThreadLocal<Boolean> APPLYING_RAMPAGE_PIERCE = ThreadLocal.withInitial(() -> false);

  private RavageurBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.RAVAGEUR)) {
      return;
    }
    applyHealthBonus(player, true);
    applyAttackDamageBonus(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.RAVAGEUR)) {
      return;
    }
    applyHealthBonus(player, false);
    applyAttackDamageBonus(player);
    refreshRampageState(player);
  }

  public static float reduceIncomingDamage(ServerPlayer player, float damage) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.RAVAGEUR)) {
      return damage;
    }
    if (isRampageActive(player)) {
      return 0.0F;
    }
    return damage * INCOMING_DAMAGE_MULTIPLIER;
  }

  public static boolean isRampageActive(ServerPlayer player) {
    Long until = RAMPAGE_UNTIL_TICK.get(player.getUUID());
    return until != null && player.level().getGameTime() < until;
  }

  public static void applyRampagePierceDamage(ServerPlayer attacker, net.minecraft.world.entity.LivingEntity target, float dealtDamage) {
    if (APPLYING_RAMPAGE_PIERCE.get()) {
      return;
    }
    if (!isRampageActive(attacker)) {
      return;
    }
    if (dealtDamage <= 0.0F) {
      return;
    }

    float pierceDamage = dealtDamage * RAMPAGE_PIERCE_MULTIPLIER;
    APPLYING_RAMPAGE_PIERCE.set(true);
    try {
      target.hurt(attacker.damageSources().magic(), pierceDamage);
    } finally {
      APPLYING_RAMPAGE_PIERCE.set(false);
    }
  }

  public static void activateCharge(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.RAVAGEUR)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - CHARGE_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < CHARGE_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((CHARGE_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.ravageur.charge.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    int broken = destroyBlocksAhead(player);
    if (broken <= 0) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.ravageur.charge.no_blocks").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    CHARGE_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 1.0F, 0.8F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.ravageur.charge.used", broken)
            .withStyle(ChatFormatting.DARK_RED),
        true
    );
  }

  public static void activateRampage(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.RAVAGEUR)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - RAMPAGE_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < RAMPAGE_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((RAMPAGE_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.ravageur.rampage.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    RAMPAGE_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    RAMPAGE_UNTIL_TICK.put(player.getUUID(), now + RAMPAGE_DURATION_TICKS);
    player.playNotifySound(SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.ravageur.rampage.used")
            .withStyle(ChatFormatting.DARK_RED),
        true
    );
  }

  public static void clear(Player player) {
    RAMPAGE_UNTIL_TICK.remove(player.getUUID());
    RAMPAGE_COOLDOWN_LAST_TICK.remove(player.getUUID());
    CHARGE_COOLDOWN_LAST_TICK.remove(player.getUUID());
    AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
    if (health != null) {
      health.removeModifier(HEALTH_ID);
    }
    AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attack != null) {
      attack.removeModifier(ATTACK_DAMAGE_ID);
    }
    if (player instanceof ServerPlayer serverPlayer) {
      endRampage(serverPlayer, false);
    }
    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
    }
  }

  private static void refreshRampageState(ServerPlayer player) {
    Long until = RAMPAGE_UNTIL_TICK.get(player.getUUID());
    if (until == null) {
      return;
    }
    if (player.level().getGameTime() < until) {
      return;
    }
    endRampage(player, true);
  }

  private static void endRampage(ServerPlayer player, boolean notify) {
    RAMPAGE_UNTIL_TICK.remove(player.getUUID());
    if (notify) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.ravageur.rampage.ended").withStyle(ChatFormatting.DARK_GRAY),
          true
      );
    }
  }

  private static int destroyBlocksAhead(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    Vec3 look = player.getLookAngle();
    Vec3 forward = new Vec3(look.x, 0.0D, look.z);
    if (forward.lengthSqr() < 1.0E-4D) {
      Direction direction = player.getDirection();
      forward = new Vec3(direction.getStepX(), 0.0D, direction.getStepZ()).normalize();
    } else {
      forward = forward.normalize();
    }
    Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();

    int broken = 0;
    BlockPos origin = player.blockPosition();
    for (int depth = 1; depth <= CHARGE_DEPTH; depth++) {
      for (int width = -(CHARGE_WIDTH / 2); width <= CHARGE_WIDTH / 2; width++) {
        Vec3 offset = forward.scale(depth).add(right.scale(width));
        for (int height = -1; height <= 1; height++) {
          BlockPos pos = origin.offset(
              (int) Math.round(offset.x),
              height,
              (int) Math.round(offset.z)
          );
          if (tryBreakBlock(level, player, pos)) {
            broken++;
          }
        }
      }
    }
    return broken;
  }

  private static boolean tryBreakBlock(ServerLevel level, ServerPlayer player, BlockPos pos) {
    BlockState state = level.getBlockState(pos);
    if (state.isAir() || state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.LAVA)) {
      return false;
    }
    if (state.getDestroySpeed(level, pos) < 0.0F) {
      return false;
    }
    return level.destroyBlock(pos, true, player);
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
}
