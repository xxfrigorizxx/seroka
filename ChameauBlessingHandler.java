package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Chameau : vitesse, step et dash du désert. */
public final class ChameauBlessingHandler {

  private static final ResourceLocation SPEED_ID = ModMain.id("chameau_speed");
  private static final ResourceLocation STEP_ID = ModMain.id("chameau_step");
  private static final double SPEED_BONUS_NORMAL = 0.10D;
  private static final double SPEED_BONUS_DESERT = 0.30D;
  /** Monte les blocs de 1,5 (0,6 vanilla + 0,9). */
  private static final double STEP_BONUS = 0.9D;
  private static final double DASH_DISTANCE = 3.0D;
  private static final int DASH_COOLDOWN_TICKS = 2 * 60 * 20;
  private static final Map<UUID, Long> DASH_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private ChameauBlessingHandler() {}

  public static void applyPassive(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CHAMEAU)) {
      return;
    }
    applyStepBonus(player);
    if (player instanceof ServerPlayer serverPlayer) {
      applySpeedBonus(serverPlayer);
    }
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CHAMEAU)) {
      return;
    }
    applyStepBonus(player);
    applySpeedBonus(player);
  }

  public static void activateDash(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.CHAMEAU)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - DASH_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < DASH_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((DASH_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.chameau.dash.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    Vec3 horizontal = player.getLookAngle();
    horizontal = new Vec3(horizontal.x, 0.0D, horizontal.z);
    if (horizontal.lengthSqr() < 1.0E-4D) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.chameau.dash.no_direction").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    Vec3 direction = horizontal.normalize();
    Vec3 destination = resolveDashDestination(player, direction);
    if (destination.distanceToSqr(player.position()) < 1.0E-4D) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.chameau.dash.blocked").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    DASH_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.teleportTo(destination.x, destination.y, destination.z);
    player.setDeltaMovement(Vec3.ZERO);
    player.fallDistance = 0.0F;
    player.hurtMarked = true;
    player.playNotifySound(SoundEvents.CAMEL_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.chameau.dash.used").withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static void clear(Player player) {
    DASH_COOLDOWN_LAST_TICK.remove(player.getUUID());
    removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
    removeModifier(player, Attributes.STEP_HEIGHT, STEP_ID);
  }

  private static void applySpeedBonus(ServerPlayer player) {
    double bonus = isInDesert(player) ? SPEED_BONUS_DESERT : SPEED_BONUS_NORMAL;
    setMultiplierModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, bonus);
  }

  private static void applyStepBonus(Player player) {
    setValueModifier(player, Attributes.STEP_HEIGHT, STEP_ID, STEP_BONUS);
  }

  private static boolean isInDesert(ServerPlayer player) {
    Holder<Biome> biome = player.level().getBiome(player.blockPosition());
    if (biome.is(BiomeTags.HAS_DESERT_PYRAMID)
        || biome.is(BiomeTags.HAS_VILLAGE_DESERT)
        || biome.is(BiomeTags.HAS_RUINED_PORTAL_DESERT)) {
      return true;
    }
    return biome.unwrapKey()
        .map(key -> key.location().getPath().contains("desert"))
        .orElse(false);
  }

  private static Vec3 resolveDashDestination(ServerPlayer player, Vec3 direction) {
    Vec3 position = player.position();
    Vec3 destination = position;
    for (int block = 0; block < (int) DASH_DISTANCE; block++) {
      Vec3 step = direction.scale(1.0D);
      Vec3 next = destination.add(step);
      if (!player.level().noCollision(player, player.getBoundingBox().move(next.subtract(destination)))) {
        break;
      }
      destination = next;
    }
    return destination;
  }

  private static void setMultiplierModifier(
      Player player,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      ResourceLocation id,
      double amount
  ) {
    AttributeInstance instance = player.getAttribute(attribute);
    if (instance == null) {
      return;
    }
    instance.removeModifier(id);
    instance.addPermanentModifier(
        new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );
  }

  private static void setValueModifier(
      Player player,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      ResourceLocation id,
      double amount
  ) {
    AttributeInstance instance = player.getAttribute(attribute);
    if (instance == null) {
      return;
    }
    instance.removeModifier(id);
    instance.addPermanentModifier(
        new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE)
    );
  }

  private static void removeModifier(
      Player player,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      ResourceLocation id
  ) {
    AttributeInstance instance = player.getAttribute(attribute);
    if (instance != null) {
      instance.removeModifier(id);
    }
  }
}
