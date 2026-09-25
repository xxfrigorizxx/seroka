package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Zombie : force accrue et chair pourrie sans danger. */
public final class ZombieBlessingHandler {

  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("zombie_attack_damage");
  private static final double ATTACK_DAMAGE_BONUS = 0.05D;
  private static final double ZOMBIE_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final Set<UUID> EATING_ROTTEN_FLESH = ConcurrentHashMap.newKeySet();

  private ZombieBlessingHandler() {}

  public static boolean hasZombieFamilyBlessing(Player player) {
    return ChimereBlessingService.hasBlessing(player, GodIds.ZOMBIE)
        || ChimereBlessingService.hasBlessing(player, GodIds.ZOMBIE_VILLAGEOIS);
  }

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
    if (!hasZombieFamilyBlessing(player)) {
      return false;
    }
    if (!isEatingRottenFlesh(player)) {
      return false;
    }
    return !effect.getEffect().value().isBeneficial();
  }

  public static boolean isProtectedFromZombies(LivingEntity entity) {
    return entity instanceof Player player && hasZombieFamilyBlessing(player);
  }

  public static void clearZombieAggro(ServerPlayer player) {
    if (!hasZombieFamilyBlessing(player)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(ZOMBIE_AGGRO_CLEAR_RADIUS);
    for (Zombie zombie : player.serverLevel().getEntitiesOfClass(Zombie.class, area)) {
      if (zombie.getTarget() == player) {
        zombie.setTarget(null);
      }
      if (zombie.getLastHurtByMob() == player) {
        zombie.setLastHurtByMob(null);
      }
    }
  }

  public static void applyPassive(Player player) {
    if (!hasZombieFamilyBlessing(player)) {
      return;
    }
    applyAttackDamageBonus(player);
    if (player instanceof ServerPlayer serverPlayer) {
      clearZombieAggro(serverPlayer);
    }
  }

  public static void refreshIfNeeded(Player player) {
    if (!hasZombieFamilyBlessing(player)) {
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
