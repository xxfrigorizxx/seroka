package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Piglin Brute : vie, hache et neutralité. */
public final class PiglinBruteBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("piglin_brute_health");
  private static final double HEALTH_BONUS = 1.0D;
  private static final double AXE_DAMAGE_MULTIPLIER = 2.0D;
  private static final double BRUTE_AXE_DAMAGE_MULTIPLIER = 5.0D;
  private static final double PIGLIN_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final int BRUTE_AXE_DURATION_TICKS = 2 * 60 * 20;
  private static final int BRUTE_AXE_COOLDOWN_TICKS = 10 * 60 * 20;
  private static final Map<UUID, Long> BRUTE_AXE_UNTIL_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> BRUTE_AXE_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private PiglinBruteBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN_BRUTE)) {
      return;
    }
    applyHealthBonus(player, true);
    clearPiglinAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN_BRUTE)) {
      return;
    }
    applyHealthBonus(player, false);
    refreshBurningRegeneration(player);
    clearPiglinAggro(player);
    refreshBruteAxeAbility(player);
  }

  public static void activateBruteAxe(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN_BRUTE)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - BRUTE_AXE_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < BRUTE_AXE_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((BRUTE_AXE_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.piglin_brute.axe.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    PiglinBruteAxeHelper.purgeAllBruteAxes(player);
    BRUTE_AXE_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    BRUTE_AXE_UNTIL_TICK.put(player.getUUID(), now + BRUTE_AXE_DURATION_TICKS);

    ItemStack axe = PiglinBruteAxeHelper.createStack();
    if (!player.getInventory().add(axe)) {
      BRUTE_AXE_COOLDOWN_LAST_TICK.remove(player.getUUID());
      BRUTE_AXE_UNTIL_TICK.remove(player.getUUID());
      player.displayClientMessage(
          Component.translatable("blessing.seroka.piglin_brute.axe.no_space").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    player.playNotifySound(SoundEvents.PIGLIN_BRUTE_ANGRY, SoundSource.PLAYERS, 1.0F, 0.9F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.piglin_brute.axe.used")
            .withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static float applyAxeDamageBonus(ServerPlayer attacker, float damage) {
    if (!ChimereBlessingService.hasBlessing(attacker, GodIds.PIGLIN_BRUTE)) {
      return damage;
    }
    if (isHoldingBruteAxe(attacker)) {
      return (float) (damage * BRUTE_AXE_DAMAGE_MULTIPLIER);
    }
    if (isHoldingAxe(attacker)) {
      return (float) (damage * AXE_DAMAGE_MULTIPLIER);
    }
    return damage;
  }

  public static boolean isBruteAxeAuthorized(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN_BRUTE)) {
      return false;
    }
    return isBruteAxeActive(player);
  }

  public static boolean isBruteAxeActive(ServerPlayer player) {
    Long until = BRUTE_AXE_UNTIL_TICK.get(player.getUUID());
    return until != null && player.level().getGameTime() < until;
  }

  public static boolean isProtectedFromPiglins(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN_BRUTE);
  }

  public static void clear(Player player) {
    BRUTE_AXE_UNTIL_TICK.remove(player.getUUID());
    BRUTE_AXE_COOLDOWN_LAST_TICK.remove(player.getUUID());
    AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
    if (health != null) {
      health.removeModifier(HEALTH_ID);
    }
    if (player instanceof ServerPlayer serverPlayer) {
      PiglinBruteAxeHelper.purgeAllBruteAxes(serverPlayer);
    }
    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
    }
  }

  public static void clearPiglinAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.PIGLIN_BRUTE)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(PIGLIN_AGGRO_CLEAR_RADIUS);
    for (Piglin piglin : player.serverLevel().getEntitiesOfClass(Piglin.class, area)) {
      clearAggroTowardPlayer(piglin, player);
    }
    for (PiglinBrute brute : player.serverLevel().getEntitiesOfClass(PiglinBrute.class, area)) {
      clearAggroTowardPlayer(brute, player);
    }
    for (ZombifiedPiglin zombified : player.serverLevel().getEntitiesOfClass(ZombifiedPiglin.class, area)) {
      clearAggroTowardPlayer(zombified, player);
    }
  }

  private static void refreshBruteAxeAbility(ServerPlayer player) {
    if (!isBruteAxeActive(player)) {
      if (BRUTE_AXE_UNTIL_TICK.containsKey(player.getUUID())) {
        endBruteAxeAbility(player, true);
      }
      return;
    }
    PiglinBruteAxeHelper.purgeOutsidePlayerInventory(player);
  }

  private static void endBruteAxeAbility(ServerPlayer player, boolean notify) {
    BRUTE_AXE_UNTIL_TICK.remove(player.getUUID());
    PiglinBruteAxeHelper.purgeAllBruteAxes(player);
    if (notify) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.piglin_brute.axe.ended").withStyle(ChatFormatting.DARK_GRAY),
          true
      );
    }
  }

  private static void refreshBurningRegeneration(ServerPlayer player) {
    if (!player.isOnFire() && !player.isInLava()) {
      return;
    }
    player.addEffect(new MobEffectInstance(
        MobEffects.REGENERATION,
        60,
        0,
        false,
        false,
        true
    ));
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

  private static void clearAggroTowardPlayer(Mob mob, ServerPlayer player) {
    if (mob.getTarget() == player) {
      mob.setTarget(null);
    }
    if (mob.getLastHurtByMob() == player) {
      mob.setLastHurtByMob(null);
    }
  }

  private static boolean isHoldingBruteAxe(Player player) {
    return PiglinBruteAxeHelper.isBruteAxe(player.getMainHandItem())
        || PiglinBruteAxeHelper.isBruteAxe(player.getOffhandItem());
  }

  private static boolean isHoldingAxe(Player player) {
    return player.getMainHandItem().getItem() instanceof AxeItem
        || player.getOffhandItem().getItem() instanceof AxeItem;
  }
}
