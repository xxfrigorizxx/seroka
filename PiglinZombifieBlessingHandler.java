package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Piglin Zombifié : horde, chair pourrie et résistance au feu. */
public final class PiglinZombifieBlessingHandler {

  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("piglin_zombifie_attack_damage");
  private static final double AURA_RADIUS = 32.0D;
  private static final double ATTACK_BONUS_PER_ALLY = 0.02D;
  private static final Set<UUID> EATING_ROTTEN_FLESH = ConcurrentHashMap.newKeySet();

  private PiglinZombifieBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN_ZOMBIFIE)) {
      return;
    }
    applyHordeStrength(player);
    extinguishIfBurning(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN_ZOMBIFIE)) {
      return;
    }
    applyHordeStrength(player);
    extinguishIfBurning(player);
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
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN_ZOMBIFIE)) {
      return false;
    }
    if (!isEatingRottenFlesh(player)) {
      return false;
    }
    return !effect.getEffect().value().isBeneficial();
  }

  public static boolean isFireOrLavaDamage(DamageSource source) {
    return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA);
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

  private static void applyHordeStrength(Player player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    int hordeSize = countHordeMembers(player, level);
    double bonus = hordeSize * ATTACK_BONUS_PER_ALLY;
    AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (instance == null) {
      return;
    }
    instance.removeModifier(ATTACK_DAMAGE_ID);
    if (bonus > 0.0D) {
      instance.addPermanentModifier(
          new AttributeModifier(ATTACK_DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
      );
    }
  }

  private static int countHordeMembers(Player player, ServerLevel level) {
    int count = 0;
    double radiusSq = AURA_RADIUS * AURA_RADIUS;
    for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
      if (other.level() != level) {
        continue;
      }
      if (player.distanceToSqr(other) > radiusSq) {
        continue;
      }
      if (ChimereBlessingService.hasBlessing(other, GodIds.PIGLIN_ZOMBIFIE)) {
        count++;
      }
    }

    AABB area = player.getBoundingBox().inflate(AURA_RADIUS);
    count += level.getEntitiesOfClass(ZombifiedPiglin.class, area).size();
    return count;
  }

  private static void extinguishIfBurning(ServerPlayer player) {
    if (player.isOnFire()) {
      player.clearFire();
    }
  }
}
