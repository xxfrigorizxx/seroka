package com.seroka.chimere;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Vindicateur : hache de fer, coups renforcés et neutralité. */
public final class VindicateurBlessingHandler {

  private static final double AXE_DAMAGE_MULTIPLIER = 1.5D;
  private static final double STRIKE_DAMAGE_MULTIPLIER = 1.5D;
  private static final int STRIKE_COOLDOWN_TICKS = 20 * 20;
  private static final double VINDICATOR_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final Map<UUID, Long> STRIKE_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Set<UUID> APPLYING_ABILITY_STRIKE = ConcurrentHashMap.newKeySet();

  private VindicateurBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VINDICATEUR)) {
      return;
    }
    ensureIronAxe(player);
    clearVindicatorAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VINDICATEUR)) {
      return;
    }
    if (player.tickCount % 40 == 0) {
      ensureIronAxe(player);
    }
    clearVindicatorAggro(player);
  }

  public static float applyAxeDamageBonus(ServerPlayer attacker, float damage) {
    if (!ChimereBlessingService.hasBlessing(attacker, GodIds.VINDICATEUR)) {
      return damage;
    }
    if (APPLYING_ABILITY_STRIKE.contains(attacker.getUUID())) {
      return damage;
    }
    if (!isHoldingAxe(attacker)) {
      return damage;
    }
    return (float) (damage * AXE_DAMAGE_MULTIPLIER);
  }

  public static void activateStrike(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VINDICATEUR)) {
      return;
    }

    if (!isHoldingAxe(player)) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.vindicateur.strike.no_axe").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - STRIKE_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < STRIKE_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((STRIKE_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.vindicateur.strike.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    LivingEntity target = findMeleeTarget(player);
    if (target == null) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.vindicateur.strike.no_target").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
    float damage = (float) (baseDamage * STRIKE_DAMAGE_MULTIPLIER * AXE_DAMAGE_MULTIPLIER);
    APPLYING_ABILITY_STRIKE.add(player.getUUID());
    try {
      target.hurt(player.damageSources().playerAttack(player), damage);
    } finally {
      APPLYING_ABILITY_STRIKE.remove(player.getUUID());
    }

    STRIKE_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.swing(InteractionHand.MAIN_HAND, true);
    player.playNotifySound(SoundEvents.VINDICATOR_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.9F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.vindicateur.strike.hit", target.getDisplayName())
            .withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static boolean isProtectedFromVindicators(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.VINDICATEUR);
  }

  public static void clearVindicatorAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VINDICATEUR)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(VINDICATOR_AGGRO_CLEAR_RADIUS);
    for (Vindicator vindicator : player.serverLevel().getEntitiesOfClass(Vindicator.class, area)) {
      if (vindicator.getTarget() == player) {
        vindicator.setTarget(null);
      }
      if (vindicator.getLastHurtByMob() == player) {
        vindicator.setLastHurtByMob(null);
      }
    }
  }

  public static void clear(Player player) {
    STRIKE_COOLDOWN_LAST_TICK.remove(player.getUUID());
    APPLYING_ABILITY_STRIKE.remove(player.getUUID());
  }

  private static void ensureIronAxe(ServerPlayer player) {
    if (hasIronAxe(player)) {
      return;
    }
    ItemStack axe = new ItemStack(Items.IRON_AXE);
    if (!player.getInventory().add(axe)) {
      player.drop(axe, false);
    }
  }

  private static boolean hasIronAxe(Player player) {
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (!stack.isEmpty() && stack.is(Items.IRON_AXE)) {
        return true;
      }
    }
    return player.getMainHandItem().is(Items.IRON_AXE) || player.getOffhandItem().is(Items.IRON_AXE);
  }

  private static boolean isHoldingAxe(Player player) {
    return player.getMainHandItem().getItem() instanceof AxeItem
        || player.getOffhandItem().getItem() instanceof AxeItem;
  }

  private static LivingEntity findMeleeTarget(ServerPlayer player) {
    double range = player.entityInteractionRange();
    Vec3 eye = player.getEyePosition(1.0F);
    Vec3 look = player.getViewVector(1.0F);
    Vec3 end = eye.add(look.scale(range));
    AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
    EntityHitResult hit = ProjectileUtil.getEntityHitResult(
        player,
        eye,
        end,
        searchBox,
        entity -> entity instanceof LivingEntity living
            && living != player
            && living.isAlive()
            && !living.isSpectator()
            && entity.isPickable(),
        range * range
    );
    if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
      return null;
    }
    return target;
  }
}
