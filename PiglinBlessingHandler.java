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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Piglin : chasse aux hoglins, or et neutralité. */
public final class PiglinBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("piglin_hoglin_health");
  private static final ResourceLocation GOLD_STRENGTH_ID = ModMain.id("piglin_gold_strength");
  private static final double PIGLIN_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final float MAX_HEALTH_PER_HOGLIN_KILL = 10.0F;
  private static final float HEAL_PER_HOGLIN_KILL = 10.0F;
  private static final double GOLD_STRENGTH_PER_STACK = 0.33D;
  private static final int GOLD_BUFF_DURATION_TICKS = 5 * 60 * 20;
  private static final Map<UUID, Integer> HOGLIN_KILLS = new ConcurrentHashMap<>();
  private static final Map<UUID, GoldBuffState> GOLD_BUFF = new ConcurrentHashMap<>();

  private PiglinBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN)) {
      return;
    }
    applyHoglinHealthBonus(player, false);
    refreshGoldStrength(player);
    extinguishIfBurning(player);
    clearPiglinAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN)) {
      return;
    }
    applyHoglinHealthBonus(player, false);
    refreshGoldStrength(player);
    extinguishIfBurning(player);
    clearPiglinAggro(player);
  }

  public static void activateGoldStrength(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN)) {
      return;
    }
    if (!consumeGoldIngot(player)) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.piglin.gold.no_ingot").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    long now = player.level().getGameTime();
    GoldBuffState current = GOLD_BUFF.get(player.getUUID());
    GoldBuffState updated;
    if (current != null && now < current.activeUntil()) {
      updated = new GoldBuffState(current.activeUntil(), current.stacks() + 1);
    } else {
      updated = new GoldBuffState(now + GOLD_BUFF_DURATION_TICKS, 1);
    }
    GOLD_BUFF.put(player.getUUID(), updated);
    applyGoldStrengthModifier(player, updated.stacks());
    player.playNotifySound(SoundEvents.PIGLIN_ADMIRING_ITEM, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable(
            "blessing.seroka.piglin.gold.used",
            (int) Math.round(updated.stacks() * 33.0D),
            formatRemainingSeconds(now, updated.activeUntil())
        ).withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static void onHoglinKilled(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN)) {
      return;
    }
    int kills = HOGLIN_KILLS.merge(player.getUUID(), 1, Integer::sum);
    applyHoglinHealthBonus(player, true);
    player.heal(HEAL_PER_HOGLIN_KILL);
    player.playNotifySound(SoundEvents.PIGLIN_CELEBRATE, SoundSource.PLAYERS, 0.8F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.piglin.hoglin.kill", kills)
            .withStyle(ChatFormatting.GREEN),
        true
    );
  }

  public static boolean isProtectedFromPiglins(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN);
  }

  public static boolean isFireOrLavaDamage(DamageSource source) {
    return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA);
  }

  public static void clear(Player player) {
    HOGLIN_KILLS.remove(player.getUUID());
    GOLD_BUFF.remove(player.getUUID());
    AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
    if (health != null) {
      health.removeModifier(HEALTH_ID);
    }
    AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attack != null) {
      attack.removeModifier(GOLD_STRENGTH_ID);
    }
    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
    }
  }

  public static void clearPiglinAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(PIGLIN_AGGRO_CLEAR_RADIUS);
    for (Piglin piglin : player.serverLevel().getEntitiesOfClass(Piglin.class, area)) {
      if (piglin.getTarget() == player) {
        piglin.setTarget(null);
      }
      if (piglin.getLastHurtByMob() == player) {
        piglin.setLastHurtByMob(null);
      }
    }
  }

  private static void refreshGoldStrength(ServerPlayer player) {
    long now = player.level().getGameTime();
    GoldBuffState buff = GOLD_BUFF.get(player.getUUID());
    if (buff == null || now >= buff.activeUntil()) {
      GOLD_BUFF.remove(player.getUUID());
      removeGoldStrengthModifier(player);
      return;
    }
    applyGoldStrengthModifier(player, buff.stacks());
  }

  private static void applyGoldStrengthModifier(ServerPlayer player, int stacks) {
    AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (instance == null) {
      return;
    }
    double bonus = stacks * GOLD_STRENGTH_PER_STACK;
    instance.removeModifier(GOLD_STRENGTH_ID);
    if (bonus > 0.0D) {
      instance.addPermanentModifier(
          new AttributeModifier(GOLD_STRENGTH_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
      );
    }
  }

  private static void removeGoldStrengthModifier(Player player) {
    AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (instance != null) {
      instance.removeModifier(GOLD_STRENGTH_ID);
    }
  }

  private static void applyHoglinHealthBonus(Player player, boolean fillNewHealth) {
    int kills = HOGLIN_KILLS.getOrDefault(player.getUUID(), 0);
    AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
    if (instance == null) {
      return;
    }

    float previousMax = player.getMaxHealth();
    float ratio = previousMax > 0.0F ? player.getHealth() / previousMax : 1.0F;
    instance.removeModifier(HEALTH_ID);
    if (kills > 0) {
      instance.addPermanentModifier(
          new AttributeModifier(
              HEALTH_ID,
              kills * MAX_HEALTH_PER_HOGLIN_KILL,
              AttributeModifier.Operation.ADD_VALUE
          )
      );
    }

    if (fillNewHealth) {
      player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + HEAL_PER_HOGLIN_KILL));
    } else {
      player.setHealth(Math.min(player.getMaxHealth(), player.getMaxHealth() * ratio));
    }
  }

  private static boolean consumeGoldIngot(ServerPlayer player) {
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (!stack.isEmpty() && stack.is(Items.GOLD_INGOT)) {
        stack.shrink(1);
        return true;
      }
    }
    return false;
  }

  private static void extinguishIfBurning(ServerPlayer player) {
    if (player.isOnFire()) {
      player.clearFire();
    }
  }

  private static int formatRemainingSeconds(long now, long activeUntil) {
    return (int) Math.ceil((activeUntil - now) / 20.0D);
  }

  private record GoldBuffState(long activeUntil, int stacks) {}
}
