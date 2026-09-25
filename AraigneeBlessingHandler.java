package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.RollMath;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Araignée : vision, escalade, toile et force accrue. */
public final class AraigneeBlessingHandler {

  private static final ResourceLocation ATTACK_DAMAGE_ID = ModMain.id("araignee_attack_damage");
  private static final double ATTACK_DAMAGE_BONUS = 0.30D;
  private static final int WEB_COOLDOWN_TICKS = 3 * 20;
  private static final double WEB_RANGE = 12.0D;
  private static final double COBWEB_SPEED_MULTIPLIER = 0.95D;
  private static final double SPIDER_AGGRO_CLEAR_RADIUS = 32.0D;
  private static final Map<UUID, Long> WEB_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private AraigneeBlessingHandler() {}

  public static void applyPassive(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ARAIGNEE)) {
      return;
    }
    setNightVision(player, true);
    applyAttackDamageBonus(player);
    clearSpiderAggro(player);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ARAIGNEE)) {
      return;
    }
    if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
      setNightVision(player, true);
    }
    applyAttackDamageBonus(player);
    AraigneeWallClimbHelper.refresh(player);
    applyCobwebMobility(player);
  }

  public static void activateWebShot(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ARAIGNEE)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - WEB_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < WEB_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((WEB_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.araignee.web.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    HitResult hit = player.pick(WEB_RANGE, 0.0F, false);
    if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.araignee.web.missed").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    BlockPos placePos = blockHit.getBlockPos().relative(blockHit.getDirection());
    BlockState current = player.level().getBlockState(placePos);
    if (!current.canBeReplaced() && !current.is(Blocks.COBWEB)) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.araignee.web.blocked").withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    player.level().setBlock(placePos, Blocks.COBWEB.defaultBlockState(), 3);
    WEB_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 0.6F, 1.4F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.araignee.web.placed").withStyle(ChatFormatting.GRAY),
        true
    );
  }

  public static boolean isProtectedFromSpiders(LivingEntity entity) {
    return entity instanceof Player player && ChimereBlessingService.hasBlessing(player, GodIds.ARAIGNEE);
  }

  public static void clearSpiderAggro(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.ARAIGNEE)) {
      return;
    }
    AABB area = player.getBoundingBox().inflate(SPIDER_AGGRO_CLEAR_RADIUS);
    for (Spider spider : player.serverLevel().getEntitiesOfClass(Spider.class, area)) {
      if (spider.getTarget() == player) {
        spider.setTarget(null);
      }
      if (spider.getLastHurtByMob() == player) {
        spider.setLastHurtByMob(null);
      }
    }
  }

  public static void clear(Player player) {
    WEB_COOLDOWN_LAST_TICK.remove(player.getUUID());
    AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (instance != null) {
      instance.removeModifier(ATTACK_DAMAGE_ID);
    }
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
    }
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

  private static void applyCobwebMobility(ServerPlayer player) {
    if (!intersectsCobweb(player)) {
      return;
    }

    if (player.zza == 0.0F && player.xxa == 0.0F) {
      return;
    }

    float speed = player.getSpeed() * (float) COBWEB_SPEED_MULTIPLIER;
    Vec3 direction = RollMath.getMovementDirection(player);
    player.setDeltaMovement(direction.x * speed, player.getDeltaMovement().y, direction.z * speed);
  }

  private static boolean intersectsCobweb(Player player) {
    AABB box = player.getBoundingBox();
    BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
    BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
    for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
      if (player.level().getBlockState(pos).is(Blocks.COBWEB)) {
        return true;
      }
    }
    return false;
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
}
