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
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Panda : endurance et résistance au bambou. */
public final class PandaBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("panda_health");
  private static final double HEALTH_BONUS = 1.0D;
  private static final int BAMBOO_FOOD_GAIN = 1;
  private static final float BAMBOO_SATURATION_GAIN = 0.5F;
  private static final float DEFENSE_PER_BAMBOO = 0.01F;
  private static final int BAMBOO_BUFF_DURATION_TICKS = 10 * 60 * 20;
  private static final Map<UUID, BambooBuffState> BAMBOO_BUFF = new ConcurrentHashMap<>();

  private PandaBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PANDA)) {
      return;
    }
    applyHealthBonus(player, true);
    refreshBambooDefense(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PANDA)) {
      return;
    }
    applyHealthBonus(player, false);
    refreshBambooDefense(player);
  }

  public static float reduceIncomingDamage(ServerPlayer player, float damage) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PANDA)) {
      return damage;
    }
    int stacks = getActiveBambooStacks(player);
    if (stacks <= 0) {
      return damage;
    }
    float multiplier = 1.0F - (stacks * DEFENSE_PER_BAMBOO);
    return damage * Math.max(0.0F, multiplier);
  }

  public static void activateEatBamboo(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PANDA)) {
      return;
    }

    FoodData foodData = player.getFoodData();
    if (foodData.getFoodLevel() >= 20) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.panda.bamboo.full").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    if (!consumeBamboo(player)) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.panda.bamboo.no_bamboo").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    foodData.eat(BAMBOO_FOOD_GAIN, BAMBOO_SATURATION_GAIN);

    long now = player.level().getGameTime();
    BambooBuffState current = BAMBOO_BUFF.get(player.getUUID());
    BambooBuffState updated;
    if (current != null && now < current.activeUntil()) {
      updated = new BambooBuffState(current.activeUntil(), current.stacks() + 1);
    } else {
      updated = new BambooBuffState(now + BAMBOO_BUFF_DURATION_TICKS, 1);
    }
    BAMBOO_BUFF.put(player.getUUID(), updated);

    player.playNotifySound(SoundEvents.PANDA_EAT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable(
            "blessing.seroka.panda.bamboo.used",
            updated.stacks(),
            formatRemainingSeconds(now, updated.activeUntil())
        ).withStyle(ChatFormatting.GREEN),
        true
    );
  }

  public static void clear(Player player) {
    BAMBOO_BUFF.remove(player.getUUID());
    AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
    if (health != null) {
      health.removeModifier(HEALTH_ID);
    }
    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
    }
  }

  private static int getActiveBambooStacks(ServerPlayer player) {
    long now = player.level().getGameTime();
    BambooBuffState buff = BAMBOO_BUFF.get(player.getUUID());
    if (buff == null || now >= buff.activeUntil()) {
      return 0;
    }
    return buff.stacks();
  }

  private static void refreshBambooDefense(ServerPlayer player) {
    long now = player.level().getGameTime();
    BambooBuffState buff = BAMBOO_BUFF.get(player.getUUID());
    if (buff == null || now >= buff.activeUntil()) {
      BAMBOO_BUFF.remove(player.getUUID());
    }
  }

  private static boolean consumeBamboo(ServerPlayer player) {
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (!stack.isEmpty() && stack.is(Items.BAMBOO)) {
        stack.shrink(1);
        return true;
      }
    }
    return false;
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

  private static int formatRemainingSeconds(long now, long activeUntil) {
    return (int) Math.ceil((activeUntil - now) / 20.0D);
  }

  private record BambooBuffState(long activeUntil, int stacks) {}
}
