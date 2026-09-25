package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Vache Champignon : rations et festin curatif. */
public final class VacheChampignonBlessingHandler {

  private static final double FEAST_RADIUS = 20.0D;
  private static final int FEAST_REGEN_DURATION_TICKS = 10 * 20;
  private static final int FEAST_REGEN_AMPLIFIER = 2;
  private static final int STEW_COOLDOWN_TICKS = 2 * 60 * 20;
  private static final int FEAST_COOLDOWN_TICKS = 10 * 60 * 20;
  private static final Map<UUID, Long> STEW_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> FEAST_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private VacheChampignonBlessingHandler() {}

  public static void activateFillStew(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VACHE_CHAMPIGNON)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - STEW_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < STEW_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((STEW_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.vache_champignon.stew.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    int filled = fillEmptySlotsWithStew(player);
    if (filled == 0) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.vache_champignon.stew.no_space").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    STEW_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.MOOSHROOM_CONVERT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.vache_champignon.stew.used", filled)
            .withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static void activateFeast(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VACHE_CHAMPIGNON)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - FEAST_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < FEAST_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((FEAST_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.vache_champignon.feast.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    AABB area = player.getBoundingBox().inflate(FEAST_RADIUS);
    int affected = 0;
    for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area)) {
      if (!entity.isAlive() || entity.isSpectator()) {
        continue;
      }
      if (entity instanceof Player targetPlayer) {
        fillFoodAndSaturation(targetPlayer);
      }
      entity.addEffect(new MobEffectInstance(
          MobEffects.REGENERATION,
          FEAST_REGEN_DURATION_TICKS,
          FEAST_REGEN_AMPLIFIER,
          false,
          true,
          true
      ));
      affected++;
    }

    if (affected == 0) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.vache_champignon.feast.no_target").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    FEAST_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.MOOSHROOM_EAT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.vache_champignon.feast.used", affected)
            .withStyle(ChatFormatting.LIGHT_PURPLE),
        true
    );
  }

  public static void clear(Player player) {
    STEW_COOLDOWN_LAST_TICK.remove(player.getUUID());
    FEAST_COOLDOWN_LAST_TICK.remove(player.getUUID());
  }

  private static int fillEmptySlotsWithStew(ServerPlayer player) {
    int filled = 0;
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (stack.isEmpty()) {
        player.getInventory().setItem(slot, new ItemStack(Items.MUSHROOM_STEW));
        filled++;
      }
    }
    return filled;
  }

  private static void fillFoodAndSaturation(Player player) {
    FoodData foodData = player.getFoodData();
    foodData.setFoodLevel(20);
    foodData.setSaturation(20.0F);
  }
}
