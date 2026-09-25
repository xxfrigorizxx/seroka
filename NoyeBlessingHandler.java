package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Noyé : force, respiration sous l'eau et immunité aux noyés. */
public final class NoyeBlessingHandler {

  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("noye_attack_damage");
  private static final double ATTACK_DAMAGE_BONUS = 0.25D;
  private static final double DROWNED_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final Set<UUID> EATING_ROTTEN_FLESH = ConcurrentHashMap.newKeySet();

  private NoyeBlessingHandler() {}

  public static boolean isRottenFlesh(ItemStack stack) {
    return !stack.isEmpty() && stack.is(Items.ROTTEN_FLESH);
  }

  public static void beginEatingRottenFlesh(ServerPlayer player) {
    EATING_ROTTEN_FLESH.add(player.getUUID());
  }

  public static void endEatingRottenFlesh(ServerPlayer player) {
    EATING_ROTTEN_FLESH.remove(player.getUUID());
  }

  public static boolean isEatingRottenFlesh(Player player) {
    return EATING_ROTTEN_FLESH.contains(player.getUUID());
  }

  public static boolean shouldBlockRottenFleshEffect(Player player, MobEffectInstance effect) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.NOYE)) {
      return false;
    }
    if (!isEatingRottenFlesh(player)) {
      return false;
    }
    return !effect.getEffect().value().isBeneficial();
  }

  public static boolean isProtectedFromDrowned(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.NOYE);
  }

  public static void clearDrownedAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.NOYE)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(DROWNED_AGGRO_CLEAR_RADIUS);
    for (Drowned drowned : player.serverLevel().getEntitiesOfClass(Drowned.class, area)) {
      if (drowned.getTarget() == player) {
        drowned.setTarget(null);
      }
      if (drowned.getLastHurtByMob() == player) {
        drowned.setLastHurtByMob(null);
      }
    }
  }

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.NOYE)) {
      return;
    }
    applyAttackDamageBonus(player);
    setWaterBreathing(player, true);
    if (player.isUnderWater()) {
      setUnderwaterNightVision(player, true);
    }
    clearDrownedAggro(player);
  }

  public static void refreshAttackIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.NOYE)) {
      return;
    }
    applyAttackDamageBonus(player);
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

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.NOYE)) {
      return;
    }
    if (!player.hasEffect(MobEffects.WATER_BREATHING)) {
      setWaterBreathing(player, true);
    }
    if (player.isUnderWater()) {
      player.setAirSupply(player.getMaxAirSupply());
      if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
        setUnderwaterNightVision(player, true);
      }
      return;
    }
    player.removeEffect(MobEffects.NIGHT_VISION);
  }

  public static void clear(Player player) {
    AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (instance != null) {
      instance.removeModifier(ATTACK_DAMAGE_ID);
    }
    if (player instanceof ServerPlayer serverPlayer) {
      endEatingRottenFlesh(serverPlayer);
      serverPlayer.removeEffect(MobEffects.WATER_BREATHING);
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
    }
  }

  private static void setUnderwaterNightVision(ServerPlayer player, boolean enabled) {
    if (!enabled) {
      player.removeEffect(MobEffects.NIGHT_VISION);
      return;
    }
    player.addEffect(new MobEffectInstance(
        MobEffects.NIGHT_VISION,
        MobEffectInstance.INFINITE_DURATION,
        0,
        false,
        false,
        true
    ));
  }

  private static void setWaterBreathing(ServerPlayer player, boolean enabled) {
    if (!enabled) {
      player.removeEffect(MobEffects.WATER_BREATHING);
      return;
    }
    player.addEffect(new MobEffectInstance(
        MobEffects.WATER_BREATHING,
        MobEffectInstance.INFINITE_DURATION,
        0,
        false,
        false,
        true
    ));
  }
}
