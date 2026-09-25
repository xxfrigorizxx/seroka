package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Husk : force, faim sur les coups et immunité aux momifiés. */
public final class HuskBlessingHandler {

  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("husk_attack_damage");
  private static final double ATTACK_DAMAGE_BONUS = 0.15D;
  private static final int HUNGER_DURATION_TICKS = 200;
  private static final double HUSK_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final Set<UUID> EATING_ROTTEN_FLESH = ConcurrentHashMap.newKeySet();

  private HuskBlessingHandler() {}

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
    if (!ChimereBlessingService.hasBlessing(player, GodIds.HUSK)) {
      return false;
    }
    if (!isEatingRottenFlesh(player)) {
      return false;
    }
    return !effect.getEffect().value().isBeneficial();
  }

  public static boolean isProtectedFromHusks(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.HUSK);
  }

  public static void clearHuskAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.HUSK)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(HUSK_AGGRO_CLEAR_RADIUS);
    for (Husk husk : player.serverLevel().getEntitiesOfClass(Husk.class, area)) {
      if (husk.getTarget() == player) {
        husk.setTarget(null);
      }
      if (husk.getLastHurtByMob() == player) {
        husk.setLastHurtByMob(null);
      }
    }
  }

  public static void applyHungerOnHit(ServerPlayer attacker, LivingEntity target) {
    if (!ChimereBlessingService.hasBlessing(attacker, GodIds.HUSK)) {
      return;
    }
    if (target == attacker) {
      return;
    }

    MobEffectInstance existing = target.getEffect(MobEffects.HUNGER);
    int amplifier = existing != null ? existing.getAmplifier() + 1 : 0;
    target.addEffect(new MobEffectInstance(
        MobEffects.HUNGER,
        HUNGER_DURATION_TICKS,
        amplifier,
        false,
        true,
        true
    ));
  }

  public static void applyPassive(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.HUSK)) {
      return;
    }
    applyAttackDamageBonus(player);
    if (player instanceof ServerPlayer serverPlayer) {
      clearHuskAggro(serverPlayer);
    }
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.HUSK)) {
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

  public static void clear(Player player) {
    AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (instance != null) {
      instance.removeModifier(ATTACK_DAMAGE_ID);
    }
    if (player instanceof ServerPlayer serverPlayer) {
      endEatingRottenFlesh(serverPlayer);
    }
  }
}
