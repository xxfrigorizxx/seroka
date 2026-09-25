package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.FactionStaminaHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Loup : viande crue comme cuite + force de meute. */
public final class LoupBlessingHandler {

  private static final ResourceLocation PACK_STRENGTH_ID = ModMain.id("loup_pack_strength");
  private static final double PACK_RADIUS = 20.0D;
  private static final double STRENGTH_PER_ALLY = 0.05D;
  private static final Set<UUID> EATING_RAW_MEAT = ConcurrentHashMap.newKeySet();

  private static final Map<Item, Item> RAW_TO_COOKED = Map.of(
      Items.BEEF, Items.COOKED_BEEF,
      Items.PORKCHOP, Items.COOKED_PORKCHOP,
      Items.CHICKEN, Items.COOKED_CHICKEN,
      Items.MUTTON, Items.COOKED_MUTTON,
      Items.RABBIT, Items.COOKED_RABBIT
  );

  private LoupBlessingHandler() {}

  public static boolean isRawMeat(ItemStack stack) {
    return !stack.isEmpty() && RAW_TO_COOKED.containsKey(stack.getItem());
  }

  public static FoodProperties getCookedFoodProperties(ItemStack rawStack, Player player) {
    Item cookedItem = RAW_TO_COOKED.get(rawStack.getItem());
    if (cookedItem == null) {
      return null;
    }
    return new ItemStack(cookedItem).getFoodProperties(player);
  }

  public static void beginEatingRawMeat(ServerPlayer player) {
    EATING_RAW_MEAT.add(player.getUUID());
  }

  public static void endEatingRawMeat(ServerPlayer player) {
    EATING_RAW_MEAT.remove(player.getUUID());
  }

  public static boolean isEatingRawMeat(Player player) {
    return EATING_RAW_MEAT.contains(player.getUUID());
  }

  public static boolean shouldBlockNegativeFoodEffect(Player player, MobEffectInstance effect) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LOUP)) {
      return false;
    }
    if (!isEatingRawMeat(player)) {
      return false;
    }
    return !effect.getEffect().value().isBeneficial();
  }

  public static void applyCookedMeal(ServerPlayer player, FoodSnapshot beforeEat, ItemStack rawStack) {
    FoodProperties cooked = getCookedFoodProperties(rawStack, player);
    if (cooked == null || beforeEat == null) {
      return;
    }

    FoodData foodData = player.getFoodData();
    int maxFood = FactionStaminaHelper.getMaxFoodLevel(player);
    int newFood = Mth.clamp(beforeEat.foodLevel() + cooked.nutrition(), 0, maxFood);
    float newSaturation = Mth.clamp(beforeEat.saturation() + cooked.saturation(), 0.0F, (float) newFood);
    foodData.setFoodLevel(newFood);
    foodData.setSaturation(newSaturation);
  }

  public static void applyPassive(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LOUP)) {
      return;
    }
    applyPackStrength(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LOUP)) {
      return;
    }
    applyPackStrength(player);
  }

  public static void clear(Player player) {
    removeModifier(player, Attributes.ATTACK_DAMAGE, PACK_STRENGTH_ID);
    if (player instanceof ServerPlayer serverPlayer) {
      endEatingRawMeat(serverPlayer);
    }
  }

  private static void applyPackStrength(Player player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    int packSize = countPackMembers(player, level);
    double bonus = packSize * STRENGTH_PER_ALLY;
    AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (instance == null) {
      return;
    }
    instance.removeModifier(PACK_STRENGTH_ID);
    if (bonus > 0.0D) {
      instance.addPermanentModifier(
          new AttributeModifier(PACK_STRENGTH_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
      );
    }
  }

  private static int countPackMembers(Player player, ServerLevel level) {
    double radiusSq = PACK_RADIUS * PACK_RADIUS;
    int count = 0;
    for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
      if (other.level() != level) {
        continue;
      }
      if (player.distanceToSqr(other) > radiusSq) {
        continue;
      }
      if (ChimereBlessingService.hasBlessing(other, GodIds.LOUP)) {
        count++;
      }
    }
    return count;
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

  public record FoodSnapshot(int foodLevel, float saturation) {
    public static FoodSnapshot from(Player player) {
      FoodData foodData = player.getFoodData();
      return new FoodSnapshot(foodData.getFoodLevel(), foodData.getSaturationLevel());
    }
  }
}
