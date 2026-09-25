package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = ModMain.MODID)
public final class LoupBlessingEvents {

  private static final Map<UUID, LoupBlessingHandler.FoodSnapshot> FOOD_BEFORE_EAT = new ConcurrentHashMap<>();

  private LoupBlessingEvents() {}

  @SubscribeEvent
  public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LOUP)) {
      return;
    }
    if (!LoupBlessingHandler.isRawMeat(event.getItem())) {
      return;
    }
    LoupBlessingHandler.beginEatingRawMeat(player);
    FOOD_BEFORE_EAT.put(player.getUUID(), LoupBlessingHandler.FoodSnapshot.from(player));
  }

  @SubscribeEvent
  public static void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      clearEating(player);
    }
  }

  @SubscribeEvent
  public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LOUP)) {
      FOOD_BEFORE_EAT.remove(player.getUUID());
      return;
    }

    if (LoupBlessingHandler.isRawMeat(event.getItem())) {
      LoupBlessingHandler.applyCookedMeal(
          player,
          FOOD_BEFORE_EAT.remove(player.getUUID()),
          event.getItem()
      );
    } else {
      FOOD_BEFORE_EAT.remove(player.getUUID());
    }
    LoupBlessingHandler.endEatingRawMeat(player);
  }

  @SubscribeEvent
  public static void onEffectApplicable(MobEffectEvent.Applicable event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    MobEffectInstance effect = event.getEffectInstance();
    if (effect == null) {
      return;
    }
    if (LoupBlessingHandler.shouldBlockNegativeFoodEffect(player, effect)) {
      event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
  }

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    if (event.getEntity() instanceof ServerPlayer player) {
      LoupBlessingHandler.refreshIfNeeded(player);
    }
  }

  private static void clearEating(ServerPlayer player) {
    LoupBlessingHandler.endEatingRawMeat(player);
    FOOD_BEFORE_EAT.remove(player.getUUID());
  }
}
