package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * Vol créatif des bénédictions Chimère (double espace ou touche V).
 * Double espace = vol vanilla ; on applique seulement la vitesse réduite et la protection de chute.
 */
public final class ChimereFlightHelper {

  private static final ResourceLocation FLIGHT_MODIFIER_ID = ModMain.id("chimere_flight");
  private static final float CREATIVE_FLY_SPEED = 0.05F;
  private static final int FLIGHT_LAND_GRACE_TICKS = 40;

  private static final float PERROQUET_HEALTH_THRESHOLD = 0.5F;
  private static final float BREEZE_HEALTH_THRESHOLD = 0.2F;
  private static final float BLAZE_HEALTH_THRESHOLD = 0.8F;
  private static final float ABEILLE_HEALTH_THRESHOLD = 0.95F;

  private ChimereFlightHelper() {}

  /** La logique Chimère ne s'applique qu'en Survie / Aventure — pas en Créatif ni Spectateur. */
  public static boolean shouldApplyChimereFlightLogic(Player player) {
    return !player.isCreative() && !player.isSpectator();
  }

  public static boolean hasPermanentFlightBlessing(String godId) {
    return GodIds.ALLAY.equals(godId)
        || GodIds.VEX.equals(godId)
        || GodIds.GHAST.equals(godId)
        || GodIds.ENDER_DRAGON.equals(godId)
        || GodIds.WITHER_SQUELETTE_BOSS.equals(godId);
  }

  public static boolean hasHealthGatedFlightBlessing(String godId) {
    return GodIds.PERROQUET.equals(godId)
        || GodIds.BREEZE.equals(godId)
        || GodIds.BLAZE.equals(godId);
  }

  public static boolean isClientManagedFlightBlessing(String godId) {
    return hasPermanentFlightBlessing(godId) || hasHealthGatedFlightBlessing(godId);
  }

  public static boolean hasFlightBlessing(Player player) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return false;
    }
    ChimereBlessing blessing = player.getData(ModAttachments.CHIMERE_BLESSING);
    if (!blessing.isActive()) {
      return false;
    }
    String godId = blessing.godId();
    return isClientManagedFlightBlessing(godId)
        || GodIds.CHAUVE_SOURIS.equals(godId)
        || GodIds.ABEILLE.equals(godId);
  }

  public static boolean isFlightModeActive(Player player) {
    return player.getData(ModAttachments.CHIMERE_FLIGHT_ACTIVE);
  }

  public static boolean meetsHealthThreshold(Player player, String godId) {
    if (player.getMaxHealth() <= 0.0F) {
      return false;
    }
    float ratio = player.getHealth() / player.getMaxHealth();
    if (GodIds.PERROQUET.equals(godId)) {
      return ratio > PERROQUET_HEALTH_THRESHOLD;
    }
    if (GodIds.BREEZE.equals(godId)) {
      return ratio > BREEZE_HEALTH_THRESHOLD;
    }
    if (GodIds.BLAZE.equals(godId)) {
      return ratio > BLAZE_HEALTH_THRESHOLD;
    }
    if (GodIds.ABEILLE.equals(godId)) {
      return ratio > ABEILLE_HEALTH_THRESHOLD;
    }
    return true;
  }

  public static boolean shouldGrantMayfly(Player player, String godId) {
    if (!hasSyncedBlessing(player, godId)) {
      return false;
    }
    if (hasPermanentFlightBlessing(godId)) {
      return true;
    }
    if (hasHealthGatedFlightBlessing(godId) || GodIds.ABEILLE.equals(godId)) {
      return meetsHealthThreshold(player, godId);
    }
    return false;
  }

  public static boolean canStartFlight(Player player) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return false;
    }

    String godId = getSyncedGodId(player);
    if (shouldGrantMayfly(player, godId)) {
      return true;
    }
    if (GodIds.CHAUVE_SOURIS.equals(godId) && hasSyncedBlessing(player, GodIds.CHAUVE_SOURIS)) {
      return ChauveSourisBlessingHandler.isFlightAllowed(player);
    }
    if (GodIds.ABEILLE.equals(godId) && hasSyncedBlessing(player, GodIds.ABEILLE)) {
      if (player instanceof ServerPlayer serverPlayer) {
        return AbeilleBlessingHandler.isFactionFlightAvailable(serverPlayer);
      }
      return meetsHealthThreshold(player, godId);
    }
    if (player instanceof ServerPlayer serverPlayer) {
      return AbeilleBlessingHandler.isFactionFlightAvailable(serverPlayer);
    }
    return false;
  }

  /** Mayfly côté client avant le double espace — ne jamais révoquer ici. */
  public static void refreshClientMayfly(Player player) {
    if (!player.level().isClientSide() || !shouldApplyChimereFlightLogic(player)) {
      return;
    }
    if (!hasFlightBlessing(player) || !canStartFlight(player)) {
      return;
    }
    grantMayfly(player);
  }

  /** Touche V : active ou coupe le vol explicitement. */
  public static void setFlightMode(Player player, boolean active) {
    if (!shouldApplyChimereFlightLogic(player)) {
      return;
    }
    if (active && !canStartFlight(player)) {
      return;
    }
    boolean wasActive = isFlightModeActive(player);
    player.setData(ModAttachments.CHIMERE_FLIGHT_ACTIVE, active);
    if (!active) {
      if (hasFlightBlessing(player) && (wasActive || player.getAbilities().flying)) {
        markSafeLanding(player);
      }
      player.getAbilities().flying = false;
      player.getAbilities().setFlyingSpeed(CREATIVE_FLY_SPEED);
      player.fallDistance = 0.0F;
      syncAbilities(player);
      return;
    }
    clearSafeLanding(player);
    grantMayfly(player);
    player.getAbilities().flying = true;
    player.getAbilities().setFlyingSpeed(getChimereFlyingSpeed(player));
    player.fallDistance = 0.0F;
    syncAbilities(player);
  }

  public static void toggleFlightMode(Player player) {
    setFlightMode(player, !isFlightModeActive(player));
  }

  /** Confirme ou coupe le vol vanilla (double espace) côté serveur. */
  public static void applyVanillaFlightState(ServerPlayer player, boolean flying) {
    if (!shouldApplyChimereFlightLogic(player) || !hasFlightBlessing(player)) {
      return;
    }
    if (flying) {
      if (!canStartFlight(player)) {
        return;
      }
      grantMayfly(player);
      player.getAbilities().flying = true;
      player.getAbilities().setFlyingSpeed(getChimereFlyingSpeed(player));
      player.fallDistance = 0.0F;
      syncAbilities(player);
      return;
    }
    if (isFlightModeActive(player)) {
      return;
    }
    if (shouldPreserveAirborneFlight(player)) {
      return;
    }
    player.getAbilities().flying = false;
    player.getAbilities().setFlyingSpeed(CREATIVE_FLY_SPEED);
    if (player.onGround()) {
      markFlightLanding(player);
    } else {
      markSafeLanding(player);
    }
    player.setData(ModAttachments.CHIMERE_FLIGHT_ACTIVE, false);
    syncAbilities(player);
  }

  /**
   * Double espace vanilla : ne force pas {@code flying}, applique seulement mayfly + vitesse Chimère.
   * Le toggle montée/descente reste 100 % vanilla.
   */
  public static void tickFlight(Player player) {
    if (!shouldApplyChimereFlightLogic(player) || !hasFlightBlessing(player)) {
      return;
    }
    refreshFlightPermission(player);
    if (!canStartFlight(player)) {
      return;
    }
    if (!player.getAbilities().flying) {
      return;
    }
    player.getAbilities().setFlyingSpeed(getChimereFlyingSpeed(player));
    player.fallDistance = 0.0F;
  }

  public static void trackFlightTransition(Player player) {
    if (!shouldApplyChimereFlightLogic(player) || !hasFlightBlessing(player)) {
      player.setData(ModAttachments.CHIMERE_WAS_FLYING_LAST_TICK, false);
      return;
    }
    boolean flying = player.getAbilities().flying;
    boolean wasFlying = player.getData(ModAttachments.CHIMERE_WAS_FLYING_LAST_TICK);
    if (wasFlying && !flying) {
      if (player.onGround()) {
        markFlightLanding(player);
      } else {
        markSafeLanding(player);
      }
      player.setData(ModAttachments.CHIMERE_FLIGHT_ACTIVE, false);
    }
    player.setData(ModAttachments.CHIMERE_WAS_FLYING_LAST_TICK, flying);
  }

  public static boolean isInFlightLandGrace(Player player) {
    long until = player.getData(ModAttachments.CHIMERE_FLIGHT_LAND_GRACE_UNTIL);
    return until > 0L && player.level().getGameTime() < until;
  }

  public static void tickFallProtection(Player player) {
    if (!shouldApplyChimereFlightLogic(player) || !hasFlightBlessing(player)) {
      return;
    }
    if (shouldProtectFromFall(player)) {
      player.fallDistance = 0.0F;
    }
  }

  public static boolean shouldProtectFromFall(Player player) {
    if (!hasFlightBlessing(player)) {
      return false;
    }
    return player.getAbilities().flying
        || isInFlightLandGrace(player)
        || expectsSafeLanding(player);
  }

  public static boolean shouldNegateFallDamage(Player player) {
    return shouldProtectFromFall(player);
  }

  public static boolean expectsSafeLanding(Player player) {
    return player.getData(ModAttachments.CHIMERE_FLIGHT_SAFE_LAND);
  }

  public static void markSafeLanding(Player player) {
    if (!hasFlightBlessing(player)) {
      return;
    }
    player.setData(ModAttachments.CHIMERE_FLIGHT_SAFE_LAND, true);
    player.fallDistance = 0.0F;
  }

  public static void clearSafeLanding(Player player) {
    player.setData(ModAttachments.CHIMERE_FLIGHT_SAFE_LAND, false);
    player.fallDistance = 0.0F;
  }

  public static void markFlightLanding(Player player) {
    if (player.level() != null) {
      player.setData(
          ModAttachments.CHIMERE_FLIGHT_LAND_GRACE_UNTIL,
          player.level().getGameTime() + FLIGHT_LAND_GRACE_TICKS
      );
    }
    player.fallDistance = 0.0F;
  }

  public static float getChimereFlyingSpeed(Player player) {
    return CREATIVE_FLY_SPEED * (float) (1.0D + getFlyingSpeedMultiplier(resolveFlightGodId(player)));
  }

  private static String resolveFlightGodId(Player player) {
    if (player instanceof ServerPlayer serverPlayer
        && AbeilleBlessingHandler.isFactionFlightAvailable(serverPlayer)
        && !hasSyncedBlessing(player, GodIds.ABEILLE)) {
      return GodIds.ABEILLE;
    }
    return getSyncedGodId(player);
  }

  /**
   * Lit le dieu depuis {@link ModAttachments#CHIMERE_BLESSING}, synchronisé serveur → client.
   */
  private static String getSyncedGodId(Player player) {
    return player.getData(ModAttachments.CHIMERE_BLESSING).godId();
  }

  /**
   * Vérifie la bénédiction via les attachments synchronisés ({@link ModAttachments#PLAYER_FACTION}
   * et {@link ModAttachments#CHIMERE_BLESSING}) pour éviter les révocations prématurées côté client.
   */
  private static boolean hasSyncedBlessing(Player player, String godId) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return false;
    }
    ChimereBlessing blessing = player.getData(ModAttachments.CHIMERE_BLESSING);
    return blessing.isActive() && godId.equals(blessing.godId());
  }

  private static double getFlyingSpeedMultiplier(String godId) {
    if (GodIds.ALLAY.equals(godId)) {
      return -0.5D;
    }
    if (GodIds.VEX.equals(godId)) {
      return -0.4D;
    }
    if (GodIds.GHAST.equals(godId)) {
      return -0.5D;
    }
    if (GodIds.PERROQUET.equals(godId)) {
      return -0.9D;
    }
    if (GodIds.ABEILLE.equals(godId)) {
      return -0.7D;
    }
    if (GodIds.ENDER_DRAGON.equals(godId)) {
      return 0.10D;
    }
    if (GodIds.WITHER_SQUELETTE_BOSS.equals(godId)) {
      return -0.05D;
    }
    return 0.0D;
  }

  public static void grantMayfly(Player player) {
    if (!shouldApplyChimereFlightLogic(player)) {
      return;
    }
    AttributeInstance instance = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
    if (instance != null && instance.getModifier(FLIGHT_MODIFIER_ID) == null) {
      instance.addPermanentModifier(
          new AttributeModifier(FLIGHT_MODIFIER_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE)
      );
    }
    if (!player.getAbilities().mayfly) {
      player.getAbilities().mayfly = true;
    }
  }

  public static void revokeMayfly(Player player) {
    revokeMayfly(player, false);
  }

  /**
   * Révoque mayfly. En vol permanent (Vex, Allay…), ne coupe pas le vol en plein air sauf si {@code force}.
   */
  public static void revokeMayfly(Player player, boolean force) {
    if (player.isCreative() || player.isSpectator()) {
      return;
    }
    if (!force && shouldPreserveAirborneFlight(player)) {
      grantMayfly(player);
      return;
    }
    AttributeInstance instance = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
    boolean hadModifier = instance != null && instance.getModifier(FLIGHT_MODIFIER_ID) != null;
    boolean wasFlying = player.getAbilities().flying;
    if (!hadModifier && !wasFlying && !player.getAbilities().mayfly) {
      return;
    }
    if (instance != null) {
      instance.removeModifier(FLIGHT_MODIFIER_ID);
    }
    player.getAbilities().mayfly = false;
    player.getAbilities().flying = false;
    player.getAbilities().setFlyingSpeed(CREATIVE_FLY_SPEED);
    syncAbilities(player);
  }

  /**
   * Après un paquet d'abilities serveur : rétablit mayfly et l'état de vol Chimère si le serveur
   * a coupé le vol à tort (désync courante en solo / latence réseau).
   */
  public static void restoreClientFlightAfterAbilitiesSync(Player player, boolean wasFlyingBeforePacket) {
    if (!player.level().isClientSide() || !shouldApplyChimereFlightLogic(player)) {
      return;
    }
    refreshClientMayfly(player);
    if (!hasFlightBlessing(player) || !canStartFlight(player)) {
      return;
    }
    boolean shouldFly = wasFlyingBeforePacket
        || isFlightModeActive(player)
        || player.getData(ModAttachments.CHIMERE_WAS_FLYING_LAST_TICK);
    if (shouldFly && !player.getAbilities().flying) {
      player.getAbilities().flying = true;
    }
    if (player.getAbilities().flying) {
      player.getAbilities().setFlyingSpeed(getChimereFlyingSpeed(player));
      player.fallDistance = 0.0F;
    }
  }

  public static void clearForPlayer(Player player) {
    player.setData(ModAttachments.CHIMERE_FLIGHT_ACTIVE, false);
    player.setData(ModAttachments.CHIMERE_FLIGHT_LAND_GRACE_UNTIL, 0L);
    clearSafeLanding(player);
    player.setData(ModAttachments.CHIMERE_WAS_FLYING_LAST_TICK, false);
    if (player.isCreative() || player.isSpectator()) {
      removeChimereFlightModifier(player);
      return;
    }
    removeChimereFlightModifier(player);
    revokeMayfly(player, true);
  }

  /**
   * Rétablit le vol vanilla Créatif / Spectateur si la logique Chimère a coupé {@code mayfly} par erreur.
   */
  public static void ensureVanillaCreativeFlight(Player player) {
    if (!player.isCreative() && !player.isSpectator()) {
      return;
    }
    removeChimereFlightModifier(player);
    if (!player.getAbilities().mayfly) {
      player.getAbilities().mayfly = true;
      syncAbilities(player);
    }
  }

  public static void refreshFlightPermission(Player player) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }
    if (!shouldApplyChimereFlightLogic(player)) {
      return;
    }
    if (player.level().isClientSide()) {
      // Attendre la synchro de la bénédiction avant toute décision — évite revokeMayfly en boucle.
      if (!player.getData(ModAttachments.CHIMERE_BLESSING).isActive()) {
        return;
      }
      refreshClientMayfly(player);
      return;
    }
    if (canStartFlight(player)) {
      grantMayfly(player);
      return;
    }
    if (shouldPreserveAirborneFlight(player)) {
      grantMayfly(player);
      return;
    }
    revokeMayfly(player);
    if (isFlightModeActive(player)) {
      setFlightMode(player, false);
    }
  }

  /** Vol permanent en l'air : ne pas couper mayfly tant que le joueur n'a pas atterri. */
  private static boolean shouldPreserveAirborneFlight(Player player) {
    if (!player.getAbilities().flying || player.onGround()) {
      return false;
    }
    String godId = getSyncedGodId(player);
    return hasSyncedBlessing(player, godId) && hasPermanentFlightBlessing(godId);
  }

  private static void removeChimereFlightModifier(Player player) {
    AttributeInstance instance = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
    if (instance != null) {
      instance.removeModifier(FLIGHT_MODIFIER_ID);
    }
  }

  private static void syncAbilities(Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.onUpdateAbilities();
    }
  }
}
