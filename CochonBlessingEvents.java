package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = ModMain.MODID)
public final class CochonBlessingEvents {

  private static final Map<UUID, Map<Holder<MobEffect>, MobEffectInstance>> EFFECTS_BEFORE_EAT =
      new ConcurrentHashMap<>();

  private CochonBlessingEvents() {}

  @SubscribeEvent
  public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (!ChimereBlessingService.hasBlessing(player, GodIds.COCHON)) {
      return;
    }
    if (!CochonBlessingHandler.isConsumable(event.getItem())) {
      return;
    }
    CochonBlessingHandler.beginConsuming(player);
    EFFECTS_BEFORE_EAT.put(player.getUUID(), snapshotEffects(player));
  }

  @SubscribeEvent
  public static void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      clearConsumption(player);
    }
  }

  @SubscribeEvent
  public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (ChimereBlessingService.hasBlessing(player, GodIds.COCHON)) {
      removeNewHarmfulEffects(player, EFFECTS_BEFORE_EAT.remove(player.getUUID()));
    } else {
      EFFECTS_BEFORE_EAT.remove(player.getUUID());
    }
    CochonBlessingHandler.endConsuming(player);
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
    if (CochonBlessingHandler.shouldBlockNegativeEffect(player, effect)) {
      event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
  }

  private static Map<Holder<MobEffect>, MobEffectInstance> snapshotEffects(ServerPlayer player) {
    Map<Holder<MobEffect>, MobEffectInstance> snapshot = new HashMap<>();
    for (MobEffectInstance effect : player.getActiveEffects()) {
      snapshot.put(effect.getEffect(), new MobEffectInstance(effect));
    }
    return snapshot;
  }

  private static void removeNewHarmfulEffects(
      ServerPlayer player,
      Map<Holder<MobEffect>, MobEffectInstance> before
  ) {
    if (before == null) {
      return;
    }

    for (MobEffectInstance after : player.getActiveEffects()) {
      if (after.getEffect().value().isBeneficial()) {
        continue;
      }

      MobEffectInstance previous = before.get(after.getEffect());
      if (previous == null || isWorse(after, previous)) {
        player.removeEffect(after.getEffect());
      }
    }
  }

  private static boolean isWorse(MobEffectInstance after, MobEffectInstance before) {
    return after.getAmplifier() > before.getAmplifier()
        || after.getDuration() > before.getDuration();
  }

  private static void clearConsumption(ServerPlayer player) {
    CochonBlessingHandler.endConsuming(player);
    EFFECTS_BEFORE_EAT.remove(player.getUUID());
  }
}
