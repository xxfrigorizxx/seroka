package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

/** Bénédiction du Dieu Léfin : vision nocturne, anti-chute, sprint x2, miaulement repoussant. */
public final class LefinBlessingHandler {

  private static final ResourceLocation SPRINT_SPEED_ID = ModMain.id("lefin_sprint_speed");
  private static final double SPRINT_SPEED_BONUS = 1.0D;
  private static final int MEOW_COOLDOWN_TICKS = 10 * 60 * 20;
  private static final int MEOW_DURATION_TICKS = 3 * 60 * 20;
  private static final double REPEL_RADIUS = 16.0D;
  private static final double REPEL_PUSH = 0.45D;

  private LefinBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LEFIN)) {
      return;
    }
    setNightVision(player, true);
    updateSprintSpeed(player);
  }

  public static void activate(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LEFIN)) {
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    long now = player.level().getGameTime();
    long elapsed = now - state.lefinMeowLastTick();
    if (elapsed < MEOW_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((MEOW_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.lefin.meow.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    long activeUntil = now + MEOW_DURATION_TICKS;
    player.setData(
        ModAttachments.CHIMERE_BLESSING_STATE,
        state.withLefinMeowLastTick(now).withLefinMeowActiveUntil(activeUntil)
    );
    player.playNotifySound(SoundEvents.CAT_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.9F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.lefin.meow.active").withStyle(ChatFormatting.GOLD),
        true
    );
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LEFIN)) {
      return;
    }

    if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
      setNightVision(player, true);
    }
    updateSprintSpeed(player);

    if (isMeowActive(player)) {
      tickRepulsion(player);
    }
  }

  public static void clear(Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
    }
    removeModifier(player, Attributes.MOVEMENT_SPEED, SPRINT_SPEED_ID);
  }

  public static boolean isMeowActive(ServerPlayer player) {
    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    return player.level().getGameTime() < state.lefinMeowActiveUntil();
  }

  private static void setNightVision(ServerPlayer player, boolean enabled) {
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

  private static void updateSprintSpeed(ServerPlayer player) {
    AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (instance == null) {
      return;
    }
    instance.removeModifier(SPRINT_SPEED_ID);
    if (player.isSprinting()) {
      instance.addPermanentModifier(
          new AttributeModifier(SPRINT_SPEED_ID, SPRINT_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
      );
    }
  }

  private static void tickRepulsion(ServerPlayer player) {
    AABB area = player.getBoundingBox().inflate(REPEL_RADIUS);
    for (Creeper creeper : player.level().getEntitiesOfClass(Creeper.class, area)) {
      repelMob(player, creeper);
    }
    for (Phantom phantom : player.level().getEntitiesOfClass(Phantom.class, area)) {
      repelMob(player, phantom);
    }
  }

  private static void repelMob(ServerPlayer player, Mob mob) {
    Vec3 away = mob.position().subtract(player.position());
    if (away.lengthSqr() < 0.01D) {
      away = new Vec3(1.0D, 0.0D, 0.0D);
    }
    Vec3 push = away.normalize().scale(REPEL_PUSH);
    mob.setDeltaMovement(mob.getDeltaMovement().add(push.x, 0.0D, push.z));
    mob.getNavigation().stop();
    mob.setTarget(null);
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
}
