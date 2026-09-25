package com.seroka.chimere;

import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.seroka.faction.RollMath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Attribution, libération et effets des bénédictions Chimère.
 */
public final class ChimereBlessingService {

  private static final Logger LOGGER = LogManager.getLogger();

  private ChimereBlessingService() {}

  public static void assignOnFactionSelect(ServerPlayer player) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }

    if (GodCatalog.ALL.isEmpty()) {
      LOGGER.debug("[Seroka] Catalogue de dieux vide — pas de tirage pour {}", player.getUUID());
      return;
    }

    GodBlessingRegistry registry = GodBlessingRegistry.get(player.server);
    registry.reconcile();

    Set<String> tried = new HashSet<>();
    while (tried.size() < GodCatalog.ALL.size()) {
      var rolled = BlessingRoller.roll(player.getRandom(), registry, tried);
      if (rolled.isEmpty()) {
        LOGGER.warn("[Seroka] Aucun dieu avec place libre pour {} (catalogue={}, essayés={})",
            player.getUUID(), GodCatalog.ALL.size(), tried);
        return;
      }

      GodDefinition god = rolled.get();
      if (!registry.tryClaim(god, player.getUUID())) {
        LOGGER.warn("[Seroka] Quota {} plein au moment du claim pour {} — nouvel essai",
            god.id(), player.getUUID());
        tried.add(god.id());
        continue;
      }

      assignBlessing(player, registry, god);
      return;
    }

    LOGGER.warn("[Seroka] Impossible d'attribuer une bénédiction à {} après {} essais",
        player.getUUID(), tried.size());
  }

  private static void assignBlessing(ServerPlayer player, GodBlessingRegistry registry, GodDefinition god) {
    registry.recordChimereRoll();
    player.setData(ModAttachments.CHIMERE_BLESSING, new ChimereBlessing(god.id()));
    player.setData(ModAttachments.CHIMERE_BLESSING_STATE, ChimereBlessingState.DEFAULT);
    applyEffects(player);

    Component godName = Component.translatable(god.translationKey());
    player.sendSystemMessage(
        Component.translatable("blessing.seroka.received", godName).withStyle(ChatFormatting.GOLD)
    );

    int remaining = god.maxBlessings() - registry.getActiveCount(god.id());
    player.sendSystemMessage(
        Component.translatable("blessing.seroka.quota_remaining", remaining, god.maxBlessings())
            .withStyle(ChatFormatting.DARK_GRAY)
    );

    int totalWeight = BlessingRoller.totalEligibleWeight(registry, Set.of());
    int godWeight = BlessingRoller.remainingCapacity(god, registry);
    double chancePercent = totalWeight > 0 ? (100.0D * godWeight / totalWeight) : 0.0D;
    LOGGER.info("[Seroka] {} reçoit la bénédiction {} ({} places restantes, poids {}/{} ≈ {} %)",
        player.getGameProfile().getName(), god.id(), remaining, godWeight, totalWeight,
        String.format("%.2f", chancePercent));
  }

  public static void release(Player player) {
    ChimereBlessing blessing = player.getData(ModAttachments.CHIMERE_BLESSING);
    if (!blessing.isActive()) {
      return;
    }

    if (player instanceof ServerPlayer serverPlayer) {
      GodBlessingRegistry.get(serverPlayer.server).release(player.getUUID());
    }

    clearEffects(player);
    player.setData(ModAttachments.CHIMERE_BLESSING, ChimereBlessing.NONE);
  }

  public static String getGodId(Player player) {
    return player.getData(ModAttachments.CHIMERE_BLESSING).godId();
  }

  private static boolean canQueryBlessings(Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      return serverPlayer.connection != null;
    }
    return true;
  }

  public static boolean hasBlessing(Player player, String godId) {
    if (!canQueryBlessings(player)) {
      return false;
    }
    return player.getData(ModAttachments.PLAYER_FACTION).isChimere()
        && godId.equals(getGodId(player));
  }

  public static void applyEffects(Player player) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      clearEffects(player);
      return;
    }
    ChimereBlessingEffects.apply(player, getGodId(player));
  }

  public static void clearEffects(Player player) {
    ChimereBlessingEffects.clear(player);
  }

  /**
   * Attribution QA/admin d'une bénédiction précise (bypass sélection aléatoire).
   *
   * @return false si le dieu est introuvable dans le catalogue
   */
  public static boolean assignBlessingForced(ServerPlayer player, String godId) {
    GodDefinition god = GodCatalog.findById(godId);
    if (god == null) {
      return false;
    }

    ChimereBlessing active = player.getData(ModAttachments.CHIMERE_BLESSING);
    if (active.isActive()) {
      GodBlessingRegistry.get(player.server).release(player.getUUID());
    }

    GodBlessingRegistry registry = GodBlessingRegistry.get(player.server);
    registry.reconcile();
    registry.release(player.getUUID());
    if (!registry.tryClaim(god, player.getUUID())) {
      LOGGER.warn("[Seroka] Quota {} plein — bénédiction {} forcée pour QA sur {}",
          god.id(), god.id(), player.getGameProfile().getName());
    }

    player.setData(ModAttachments.CHIMERE_BLESSING, new ChimereBlessing(god.id()));
    player.setData(ModAttachments.CHIMERE_BLESSING_STATE, ChimereBlessingState.DEFAULT);
    applyEffects(player);
    return true;
  }

  /** Réapplique les bonus d'attaque des bénédictions (après changement d'équipement). */
  public static void refreshAttackPassives(Player player) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }
    String godId = getGodId(player);
    if (GodIds.ZOMBIE.equals(godId)) {
      ZombieBlessingHandler.refreshIfNeeded(player);
      return;
    }
    if (GodIds.HUSK.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      HuskBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.NOYE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      NoyeBlessingHandler.refreshAttackIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.ARAIGNEE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      AraigneeBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.ARAIGNEE_EMPOISONNEE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      AraigneeEmpoisonneeBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.CREEPER.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      CreeperBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.CREEPER_CHARGE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      CreeperChargeBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.OURS_POLAIRE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      OursPolaireBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.RAVAGEUR.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      RavageurBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.PHANTOME.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      PhantomeBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.OURS_POLAIRE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      OursPolaireBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.LAMA.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      LamaBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.CHAMEAU.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      ChameauBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.POISSON_ARGENT.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      PoissonArgentBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.VINDICATEUR.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      VindicateurBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.PILLARD.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      PillardBlessingHandler.refreshIfNeeded(serverPlayer);
      return;
    }
    if (GodIds.EVOCATEUR.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      EvocateurBlessingHandler.refreshIfNeeded(serverPlayer);
    }
  }

  /** Clic molette (Chimère) — déclenche l'effet du dieu actif. */
  public static void activateAbility(ServerPlayer player) {
    activateAbility(player, player.isShiftKeyDown());
  }

  public static void activateAbility(ServerPlayer player, boolean sneaking) {
    activateAbility(player, sneaking, 0.0F, 0.0F);
  }

  public static void activateAbility(ServerPlayer player, boolean sneaking, float lookX, float lookZ) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }
    String godId = getGodId(player);
    if (GodIds.EVOCATEUR.equals(godId)) {
      if (sneaking && EvocateurBlessingHandler.tryDyeBlueSheep(player)) {
        return;
      }
      EvocateurBlessingHandler.activateVexSummon(player);
      return;
    }
    if (GodIds.VACHE.equals(godId)) {
      VacheBlessingHandler.activate(player);
      return;
    }
    if (GodIds.MOUTON.equals(godId)) {
      MoutonBlessingHandler.activate(player);
      return;
    }
    if (GodIds.POULET.equals(godId)) {
      PouletBlessingHandler.activate(player);
      return;
    }
    if (GodIds.LAPIN.equals(godId)) {
      LapinBlessingHandler.activate(player);
      return;
    }
    if (GodIds.CHEVAL.equals(godId)) {
      ChevalBlessingHandler.activate(player);
      return;
    }
    if (GodIds.ANE.equals(godId)) {
      AneBlessingHandler.activate(player);
      return;
    }
    if (GodIds.MULE.equals(godId)) {
      MuleBlessingHandler.activate(player);
      return;
    }
    if (GodIds.RENARD.equals(godId)) {
      RenardBlessingHandler.activate(player);
      return;
    }
    if (GodIds.LEFIN.equals(godId)) {
      LefinBlessingHandler.activate(player);
      return;
    }
    if (GodIds.POISSON_GLOBE.equals(godId)) {
      PoissonGlobeBlessingHandler.activate(player);
      return;
    }
    if (GodIds.CALAMAR.equals(godId)) {
      CalamarBlessingHandler.activateInk(player);
      return;
    }
    if (GodIds.CALAMAR_LUMINESCENT.equals(godId)) {
      CalamarLuminescentBlessingHandler.activateGlowing(player);
      return;
    }
    if (GodIds.TORTUE.equals(godId)) {
      TortueBlessingHandler.activate(player);
      return;
    }
    if (GodIds.DAUPHIN.equals(godId)) {
      DauphinBlessingHandler.activateImpulse(player);
      return;
    }
    if (GodIds.AXOLOTL.equals(godId)) {
      AxolotlBlessingHandler.activateRegen(player);
      return;
    }
    if (GodIds.GRENOUILLE.equals(godId)) {
      GrenouilleBlessingHandler.activateJump(player);
      return;
    }
    if (GodIds.VAGABOND.equals(godId)) {
      VagabondBlessingHandler.activateSlowArrows(player);
      return;
    }
    if (GodIds.EMBOURBE.equals(godId)) {
      EmbourbeBlessingHandler.activatePoisonArrows(player);
      return;
    }
    if (GodIds.ARAIGNEE.equals(godId)) {
      AraigneeBlessingHandler.activateWebShot(player);
      return;
    }
    if (GodIds.ARAIGNEE_EMPOISONNEE.equals(godId)) {
      AraigneeEmpoisonneeBlessingHandler.activateWebShot(player);
      return;
    }
    if (GodIds.CREEPER.equals(godId)) {
      CreeperBlessingHandler.activateExplosion(player);
      return;
    }
    if (GodIds.CREEPER_CHARGE.equals(godId)) {
      CreeperChargeBlessingHandler.activateExplosion(player);
      return;
    }
    if (GodIds.SORCIERE.equals(godId)) {
      SorciereBlessingHandler.activateThrowPotion(player);
      return;
    }
    if (GodIds.OURS_POLAIRE.equals(godId)) {
      OursPolaireBlessingHandler.activateSmash(player);
      return;
    }
    if (GodIds.LAMA.equals(godId)) {
      LamaBlessingHandler.activateSpit(player);
      return;
    }
    if (GodIds.CHEVRE.equals(godId)) {
      ChevreBlessingHandler.activateCharge(player, resolveHorizontalLook(player, lookX, lookZ));
      return;
    }
    if (GodIds.CHAMEAU.equals(godId)) {
      ChameauBlessingHandler.activateDash(player);
      return;
    }
    if (GodIds.PHANTOME.equals(godId)) {
      PhantomeBlessingHandler.toggleGlide(player);
      return;
    }
    if (GodIds.CHAUVE_SOURIS.equals(godId)) {
      ChauveSourisBlessingHandler.activateNightVision(player);
      return;
    }
    if (GodIds.POISSON_ARGENT.equals(godId)) {
      PoissonArgentBlessingHandler.activateBurrow(player);
      return;
    }
    if (GodIds.VINDICATEUR.equals(godId)) {
      VindicateurBlessingHandler.activateStrike(player);
      return;
    }
    if (GodIds.PILLARD.equals(godId)) {
      PillardBlessingHandler.activateSummon(player);
      return;
    }
    if (GodIds.RAVAGEUR.equals(godId)) {
      RavageurBlessingHandler.activateRampage(player);
      return;
    }
    if (GodIds.GHAST.equals(godId)) {
      GhastBlessingHandler.activateFireball(player);
      return;
    }
    if (GodIds.PIGLIN.equals(godId)) {
      PiglinBlessingHandler.activateGoldStrength(player);
      return;
    }
    if (GodIds.PIGLIN_BRUTE.equals(godId)) {
      PiglinBruteBlessingHandler.activateBruteAxe(player);
      return;
    }
    if (GodIds.BLAZE.equals(godId)) {
      BlazeBlessingHandler.activateFireball(player);
      return;
    }
    if (GodIds.WITHER_SQUELETTE_BOSS.equals(godId)) {
      WitherSqueletteBossBlessingHandler.activateWitherSkull(player);
      return;
    }
    if (GodIds.ENDER_DRAGON.equals(godId)) {
      EnderDragonBlessingHandler.toggleBreath(player);
      return;
    }
    if (GodIds.MAGMA_CUBE.equals(godId)) {
      MagmaCubeBlessingHandler.activateJump(player);
      return;
    }
    if (GodIds.GARDIEN.equals(godId)) {
      GardienBlessingHandler.activateBeam(player);
      return;
    }
    if (GodIds.GRAND_GARDIEN.equals(godId)) {
      GrandGardienBlessingHandler.activateBeam(player);
      return;
    }
    if (GodIds.BREEZE.equals(godId)) {
      BreezeBlessingHandler.activateWindCharge(player);
      return;
    }
    if (GodIds.TATOU.equals(godId)) {
      TatouBlessingHandler.activateShell(player);
      return;
    }
    if (GodIds.RENIFLEUR.equals(godId)) {
      RenifleurBlessingHandler.activateDig(player);
      return;
    }
    if (GodIds.WARDEN.equals(godId)) {
      WardenBlessingHandler.activateSonicBoom(player);
      return;
    }
    if (GodIds.ENDERMAN.equals(godId)) {
      EndermanBlessingHandler.activateTeleport(player);
      return;
    }
    if (GodIds.SHULKER.equals(godId)) {
      ShulkerBlessingHandler.activateBullet(player);
      return;
    }
    if (GodIds.ENDERMITE.equals(godId)) {
      EndermiteBlessingHandler.activateTeleport(player);
      return;
    }
    if (GodIds.ABEILLE.equals(godId)) {
      AbeilleBlessingHandler.activateSting(player);
      return;
    }
    if (GodIds.VEX.equals(godId)) {
      VexBlessingHandler.activateIntangibility(player);
      return;
    }
    if (GodIds.PANDA.equals(godId)) {
      PandaBlessingHandler.activateEatBamboo(player);
      return;
    }
    if (GodIds.VACHE_CHAMPIGNON.equals(godId)) {
      VacheChampignonBlessingHandler.activateFeast(player);
      return;
    }
    if (GodIds.GOLEM_FER.equals(godId)) {
      GolemFerBlessingHandler.activateSlam(player);
      return;
    }
    if (GodIds.GOLEM_NEIGE.equals(godId)) {
      GolemNeigeBlessingHandler.activateSnowball(player);
    }
  }

  /** Touche G (Chimère) — déclenche la capacité secondaire du dieu actif. */
  public static void activateSecondaryAbility(ServerPlayer player) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }
    String godId = getGodId(player);
    if (GodIds.CALAMAR.equals(godId)) {
      CalamarBlessingHandler.activateBoost(player);
      return;
    }
    if (GodIds.CALAMAR_LUMINESCENT.equals(godId)) {
      CalamarLuminescentBlessingHandler.activateBoost(player);
      return;
    }
    if (GodIds.AXOLOTL.equals(godId)) {
      AxolotlBlessingHandler.activatePlayDead(player);
      return;
    }
    if (GodIds.SORCIERE.equals(godId)) {
      SorciereBlessingHandler.activateSelfPotion(player);
      return;
    }
    if (GodIds.LAMA.equals(godId)) {
      LamaBlessingHandler.activatePack(player);
      return;
    }
    if (GodIds.CHEVRE.equals(godId)) {
      ChevreBlessingHandler.activateJump(player);
      return;
    }
    if (GodIds.PILLARD.equals(godId)) {
      PillardBlessingHandler.activateArrows(player);
      return;
    }
    if (GodIds.EVOCATEUR.equals(godId)) {
      EvocateurBlessingHandler.activateLineFangs(player);
      return;
    }
    if (GodIds.RAVAGEUR.equals(godId)) {
      RavageurBlessingHandler.activateCharge(player);
      return;
    }
    if (GodIds.GRAND_GARDIEN.equals(godId)) {
      GrandGardienBlessingHandler.activateMiningFatigue(player);
      return;
    }
    if (GodIds.ABEILLE.equals(godId)) {
      AbeilleBlessingHandler.activateSpawnBees(player);
      return;
    }
    if (GodIds.VACHE_CHAMPIGNON.equals(godId)) {
      VacheChampignonBlessingHandler.activateFillStew(player);
      return;
    }
    if (GodIds.ILLUSIONNISTE.equals(godId)) {
      IllusionnisteBlessingHandler.activateIllusions(player);
      return;
    }
    if (GodIds.ENDER_DRAGON.equals(godId)) {
      EnderDragonBlessingHandler.activateFireball(player);
    }
  }

  static Vec3 resolveHorizontalLook(ServerPlayer player, float lookX, float lookZ) {
    Vec3 fromClient = new Vec3(lookX, 0.0D, lookZ);
    if (fromClient.lengthSqr() > 1.0E-4D) {
      return fromClient.normalize();
    }
    if (RollMath.hasMovementInput(player)) {
      return RollMath.getMovementDirection(player);
    }
    Vec3 look = player.getLookAngle();
    Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
    if (horizontal.lengthSqr() > 1.0E-4D) {
      return horizontal.normalize();
    }
    return Vec3.directionFromRotation(0.0F, player.getYRot());
  }

  /** Touche H (Chimère) — déclenche la capacité tertiaire du dieu actif. */
  public static void activateTertiaryAbility(ServerPlayer player) {
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }
    String godId = getGodId(player);
    if (GodIds.EVOCATEUR.equals(godId)) {
      EvocateurBlessingHandler.activateCircleFangs(player);
    }
  }
}
