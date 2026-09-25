package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Sorcière : potions positives (G) et négatives (clic molette). */
public final class SorciereBlessingHandler {

  private static final int SELF_POTION_COOLDOWN_TICKS = 60 * 20;
  private static final int THROW_POTION_COOLDOWN_TICKS = 45 * 20;
  private static final float THROW_VELOCITY = 0.75F;
  private static final float THROW_INACCURACY = 8.0F;
  private static final List<Holder<Potion>> POSITIVE_POTIONS = List.of(
      Potions.HEALING,
      Potions.FIRE_RESISTANCE,
      Potions.SWIFTNESS,
      Potions.WATER_BREATHING
  );
  private static final List<Holder<Potion>> NEGATIVE_POTIONS = List.of(
      Potions.POISON,
      Potions.SLOWNESS,
      Potions.WEAKNESS,
      Potions.HARMING
  );
  private static final Map<UUID, Long> SELF_POTION_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> THROW_POTION_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private SorciereBlessingHandler() {}

  public static void activateSelfPotion(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SORCIERE)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - SELF_POTION_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < SELF_POTION_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((SELF_POTION_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.sorciere.self.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    Holder<Potion> potion = POSITIVE_POTIONS.get(player.getRandom().nextInt(POSITIVE_POTIONS.size()));
    applyPotionEffects(player, potion);
    SELF_POTION_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.sorciere.self.applied", describePotion(potion))
            .withStyle(ChatFormatting.LIGHT_PURPLE),
        true
    );
  }

  public static void activateThrowPotion(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.SORCIERE)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - THROW_POTION_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < THROW_POTION_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((THROW_POTION_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.sorciere.throw.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    Holder<Potion> potion = NEGATIVE_POTIONS.get(player.getRandom().nextInt(NEGATIVE_POTIONS.size()));
    throwSplashPotion(player, potion);
    THROW_POTION_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.WITCH_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.sorciere.throw.launched", describePotion(potion))
            .withStyle(ChatFormatting.DARK_PURPLE),
        true
    );
  }

  public static void clear(Player player) {
    SELF_POTION_COOLDOWN_LAST_TICK.remove(player.getUUID());
    THROW_POTION_COOLDOWN_LAST_TICK.remove(player.getUUID());
  }

  private static void applyPotionEffects(ServerPlayer player, Holder<Potion> potion) {
    for (MobEffectInstance effect : potion.value().getEffects()) {
      player.addEffect(new MobEffectInstance(effect));
    }
  }

  private static void throwSplashPotion(ServerPlayer player, Holder<Potion> potion) {
    ItemStack stack = PotionContents.createItemStack(Items.SPLASH_POTION, potion);
    ThrownPotion projectile = new ThrownPotion(player.level(), player);
    projectile.setItem(stack);
    Vec3 look = player.getViewVector(1.0F);
    projectile.shoot(look.x, look.y, look.z, THROW_VELOCITY, THROW_INACCURACY);
    player.level().addFreshEntity(projectile);
  }

  private static Component describePotion(Holder<Potion> potion) {
    return Component.translatable(Potion.getName(Optional.of(potion), ""));
  }
}
