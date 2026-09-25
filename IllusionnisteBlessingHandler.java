package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.entity.PlayerIllusionEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Illusionniste : double PV et clones trompeurs. */
public final class IllusionnisteBlessingHandler {

  private static final ResourceLocation HEALTH_ID = ModMain.id("illusionniste_health");
  private static final double HEALTH_BONUS = 1.0D;
  private static final int ILLUSION_COUNT = 4;
  private static final double MIN_SPAWN_DISTANCE = 4.0D;
  private static final double MAX_SPAWN_DISTANCE = 10.0D;
  private static final int ILLUSION_DURATION_TICKS = 2 * 60 * 20;
  private static final int ILLUSION_COOLDOWN_TICKS = 5 * 60 * 20;

  private static final Map<UUID, Long> ILLUSION_UNTIL_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> ILLUSION_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, List<UUID>> ILLUSION_ENTITY_IDS = new ConcurrentHashMap<>();

  private IllusionnisteBlessingHandler() {}

  public static boolean isIllusionActive(ServerPlayer player) {
    Long until = ILLUSION_UNTIL_TICK.get(player.getUUID());
    return until != null && player.level().getGameTime() < until;
  }

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ILLUSIONNISTE)) {
      return;
    }
    applyHealthBonus(player, true);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ILLUSIONNISTE)) {
      return;
    }
    applyHealthBonus(player, false);
    tickIllusion(player);
  }

  public static void activateIllusions(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ILLUSIONNISTE)) {
      return;
    }

    long now = player.level().getGameTime();
    if (isIllusionActive(player)) {
      return;
    }

    long elapsed = now - ILLUSION_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < ILLUSION_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((ILLUSION_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.illusionniste.illusion.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    ILLUSION_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    ILLUSION_UNTIL_TICK.put(player.getUUID(), now + ILLUSION_DURATION_TICKS);
    applyInvisibleState(player, true);
    spawnIllusions(player);
    spawnActivationParticles(player);
    player.playNotifySound(SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable(
            "blessing.seroka.illusionniste.illusion.used",
            ILLUSION_DURATION_TICKS / 20
        ).withStyle(ChatFormatting.LIGHT_PURPLE),
        true
    );
  }

  public static void clear(ServerPlayer player) {
    endIllusions(player, false);
    ILLUSION_UNTIL_TICK.remove(player.getUUID());
    ILLUSION_COOLDOWN_LAST_TICK.remove(player.getUUID());
    removeHealthBonus(player);
  }

  private static void tickIllusion(ServerPlayer player) {
    if (!isIllusionActive(player)) {
      if (ILLUSION_ENTITY_IDS.containsKey(player.getUUID()) || player.isInvisible()) {
        endIllusions(player, false);
      }
      return;
    }

    applyInvisibleState(player, true);
    cleanupDeadIllusions(player);
    if (getIllusionEntities(player).isEmpty()) {
      spawnIllusions(player);
    }
  }

  private static void endIllusions(ServerPlayer player, boolean startCooldown) {
    long now = player.level().getGameTime();
    if (startCooldown) {
      ILLUSION_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    }
    ILLUSION_UNTIL_TICK.remove(player.getUUID());
    removeIllusionEntities(player);
    restorePlayerAppearance(player);
  }

  private static void spawnIllusions(ServerPlayer player) {
    removeIllusionEntities(player);
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }

    List<UUID> illusionIds = new ArrayList<>(ILLUSION_COUNT);
    for (int index = 0; index < ILLUSION_COUNT; index++) {
      double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
      double distance = MIN_SPAWN_DISTANCE + player.getRandom().nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
      double offsetX = Math.cos(angle) * distance;
      double offsetZ = Math.sin(angle) * distance;
      PlayerIllusionEntity illusion = PlayerIllusionEntity.spawn(serverLevel, player, offsetX, 0.0D, offsetZ);
      if (illusion != null) {
        illusionIds.add(illusion.getUUID());
      }
    }
    ILLUSION_ENTITY_IDS.put(player.getUUID(), illusionIds);
  }

  private static void removeIllusionEntities(ServerPlayer player) {
    List<UUID> illusionIds = ILLUSION_ENTITY_IDS.remove(player.getUUID());
    if (illusionIds == null || !(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    for (UUID illusionId : illusionIds) {
      var entity = serverLevel.getEntity(illusionId);
      if (entity != null) {
        entity.discard();
      }
    }
  }

  private static void cleanupDeadIllusions(ServerPlayer player) {
    List<UUID> illusionIds = ILLUSION_ENTITY_IDS.get(player.getUUID());
    if (illusionIds == null) {
      return;
    }
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    Iterator<UUID> iterator = illusionIds.iterator();
    while (iterator.hasNext()) {
      UUID illusionId = iterator.next();
      if (serverLevel.getEntity(illusionId) == null) {
        iterator.remove();
      }
    }
  }

  private static List<UUID> getIllusionEntities(ServerPlayer player) {
    return ILLUSION_ENTITY_IDS.getOrDefault(player.getUUID(), List.of());
  }

  private static void applyInvisibleState(ServerPlayer player, boolean invisible) {
    player.setInvisible(invisible);
    if (invisible) {
      if (!player.hasEffect(MobEffects.INVISIBILITY)) {
        player.addEffect(new MobEffectInstance(
            MobEffects.INVISIBILITY,
            MobEffectInstance.INFINITE_DURATION,
            0,
            false,
            false,
            false
        ));
      }
      return;
    }
    restorePlayerAppearance(player);
  }

  private static void restorePlayerAppearance(ServerPlayer player) {
    player.setInvisible(false);
    if (player.hasEffect(MobEffects.INVISIBILITY)) {
      player.removeEffect(MobEffects.INVISIBILITY);
    }
  }

  private static void spawnActivationParticles(ServerPlayer player) {
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    serverLevel.sendParticles(
        ParticleTypes.PORTAL,
        player.getX(),
        player.getY() + player.getBbHeight() * 0.5D,
        player.getZ(),
        40,
        1.0D,
        0.5D,
        1.0D,
        0.1D
    );
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

  private static void removeHealthBonus(Player player) {
    AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
    if (instance != null) {
      instance.removeModifier(HEALTH_ID);
    }
  }
}
