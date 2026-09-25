package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Renifleur : endurance, résistance et fouille. */
public final class RenifleurBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("renifleur_health");
  private static final double HEALTH_BONUS = 1.0D;
  private static final float INCOMING_DAMAGE_MULTIPLIER = 0.5F;
  private static final int DIG_COOLDOWN_TICKS = 5 * 60 * 20;
  private static final Map<UUID, Long> DIG_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private RenifleurBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.RENIFLEUR)) {
      return;
    }
    applyHealthBonus(player, true);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.RENIFLEUR)) {
      return;
    }
    applyHealthBonus(player, false);
  }

  public static float reduceIncomingDamage(ServerPlayer player, float damage) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.RENIFLEUR)) {
      return damage;
    }
    return damage * INCOMING_DAMAGE_MULTIPLIER;
  }

  public static void activateDig(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.RENIFLEUR)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - DIG_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < DIG_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((DIG_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.renifleur.dig.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    ItemStack seeds = new ItemStack(Items.TORCHFLOWER_SEEDS);
    ItemStack pod = new ItemStack(Items.PITCHER_POD);
    boolean addedSeeds = player.getInventory().add(seeds);
    boolean addedPod = player.getInventory().add(pod);
    if (!addedSeeds) {
      player.drop(seeds, false);
    }
    if (!addedPod) {
      player.drop(pod, false);
    }

    DIG_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.SNIFFER_DIGGING, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.renifleur.dig.used").withStyle(ChatFormatting.GREEN),
        true
    );
  }

  public static void clear(Player player) {
    AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
    if (health != null) {
      health.removeModifier(HEALTH_ID);
    }
    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
    }
    DIG_COOLDOWN_LAST_TICK.remove(player.getUUID());
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
}
