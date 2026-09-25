package com.seroka.chimere;

import com.seroka.faction.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Effets de bénédiction par dieu — brancher chaque dieu ici au fil des tests. */
public final class ChimereBlessingEffects {

  private ChimereBlessingEffects() {}

  public static void apply(Player player, String godId) {
    clear(player);
    if (!player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
      return;
    }
    if (GodIds.POULET.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
      if (state.pouletSlowFall()) {
        PouletBlessingHandler.setSlowFall(serverPlayer, true);
      }
    }
    if (GodIds.LAPIN.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
      if (state.lapinAgility()) {
        LapinBlessingHandler.setAgility(serverPlayer, true);
      }
    }
    if (GodIds.CHEVAL.equals(godId)) {
      ChevalBlessingHandler.applyPassive(player);
    }
    if (GodIds.ANE.equals(godId)) {
      AneBlessingHandler.applyPassive(player);
    }
    if (GodIds.MULE.equals(godId)) {
      MuleBlessingHandler.applyPassive(player);
    }
    if (GodIds.LOUP.equals(godId)) {
      LoupBlessingHandler.applyPassive(player);
    }
    if (GodIds.RENARD.equals(godId)) {
      RenardBlessingHandler.applyPassive(player);
    }
    if (GodIds.LEFIN.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      LefinBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.MORUE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      MorueBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.SAUMON.equals(godId)) {
      SaumonBlessingHandler.applyPassive(player);
    }
    if (GodIds.POISSON_TROPICAL.equals(godId)) {
      PoissonTropicalBlessingHandler.applyPassive(player);
    }
    if (GodIds.POISSON_GLOBE.equals(godId)) {
      PoissonGlobeBlessingHandler.applyPassive(player);
    }
    if (GodIds.CALAMAR.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      CalamarBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.CALAMAR_LUMINESCENT.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      CalamarLuminescentBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.TORTUE.equals(godId)) {
      TortueBlessingHandler.applyPassive(player);
    }
    if (GodIds.DAUPHIN.equals(godId)) {
      DauphinBlessingHandler.applyPassive(player);
    }
    if (GodIds.AXOLOTL.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      AxolotlBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.GRENOUILLE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      GrenouilleBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.TETARD.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      TetardBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.ZOMBIE.equals(godId)) {
      ZombieBlessingHandler.applyPassive(player);
    }
    if (GodIds.ZOMBIE_VILLAGEOIS.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      ZombieVillageoisBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.HUSK.equals(godId)) {
      HuskBlessingHandler.applyPassive(player);
    }
    if (GodIds.NOYE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      NoyeBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.SQUELETTE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      SqueletteBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.VAGABOND.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      VagabondBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.EMBOURBE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      EmbourbeBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.ARAIGNEE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      AraigneeBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.ARAIGNEE_EMPOISONNEE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      AraigneeEmpoisonneeBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.CREEPER.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      CreeperBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.CREEPER_CHARGE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      CreeperChargeBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.PHANTOME.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      PhantomeBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.OURS_POLAIRE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      OursPolaireBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.LAMA.equals(godId)) {
      LamaBlessingHandler.applyPassive(player);
    }
    if (GodIds.CHAMEAU.equals(godId)) {
      ChameauBlessingHandler.applyPassive(player);
    }
    if (GodIds.CHAUVE_SOURIS.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      ChauveSourisBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.VINDICATEUR.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      VindicateurBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.PILLARD.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      PillardBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.EVOCATEUR.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      EvocateurBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.RAVAGEUR.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      RavageurBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.PERROQUET.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      PerroquetBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.GHAST.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      GhastBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.PIGLIN_ZOMBIFIE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      PiglinZombifieBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.PIGLIN.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      PiglinBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.PIGLIN_BRUTE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      PiglinBruteBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.ZOGLIN.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      ZoglinBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.ARPENTEUR.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      ArpenteurBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.BLAZE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      BlazeBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.WITHER_SQUELETTE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      WitherSqueletteBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.WITHER_SQUELETTE_BOSS.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      WitherSqueletteBossBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.MAGMA_CUBE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      MagmaCubeBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.GARDIEN.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      GardienBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.GRAND_GARDIEN.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      GrandGardienBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.BREEZE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      BreezeBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.SLIME.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      SlimeBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.RENIFLEUR.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      RenifleurBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.WARDEN.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      WardenBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.ENDERMAN.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      EndermanBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.SHULKER.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      ShulkerBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.ENDERMITE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      EndermiteBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.ABEILLE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      AbeilleBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.ALLAY.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      AllayBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.VEX.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      VexBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.PANDA.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      PandaBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.GOLEM_FER.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      GolemFerBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.VILLAGEOIS.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      VillageoisBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.ILLUSIONNISTE.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      IllusionnisteBlessingHandler.applyPassive(serverPlayer);
    }
    if (GodIds.ENDER_DRAGON.equals(godId) && player instanceof ServerPlayer serverPlayer) {
      EnderDragonBlessingHandler.applyPassive(serverPlayer);
    }
  }

  public static void clear(Player player) {
    ChimereFlightHelper.clearForPlayer(player);
    if (player instanceof ServerPlayer serverPlayer) {
      PouletBlessingHandler.clear(serverPlayer);
      LapinBlessingHandler.clear(serverPlayer);
    }
    ChevalBlessingHandler.clear(player);
    AneBlessingHandler.clear(player);
    MuleBlessingHandler.clear(player);
    LoupBlessingHandler.clear(player);
    RenardBlessingHandler.clear(player);
    LefinBlessingHandler.clear(player);
    MorueBlessingHandler.clear(player);
    SaumonBlessingHandler.clear(player);
    PoissonTropicalBlessingHandler.clear(player);
    PoissonGlobeBlessingHandler.clear(player);
    CalamarBlessingHandler.clear(player);
    CalamarLuminescentBlessingHandler.clear(player);
    TortueBlessingHandler.clear(player);
    DauphinBlessingHandler.clear(player);
    AxolotlBlessingHandler.clear(player);
    GrenouilleBlessingHandler.clear(player);
    TetardBlessingHandler.clear(player);
    ZombieBlessingHandler.clear(player);
    ZombieVillageoisBlessingHandler.clear(player);
    HuskBlessingHandler.clear(player);
    NoyeBlessingHandler.clear(player);
    SqueletteBlessingHandler.clear(player);
    VagabondBlessingHandler.clear(player);
    EmbourbeBlessingHandler.clear(player);
    AraigneeBlessingHandler.clear(player);
    AraigneeEmpoisonneeBlessingHandler.clear(player);
    CreeperBlessingHandler.clear(player);
    CreeperChargeBlessingHandler.clear(player);
    SorciereBlessingHandler.clear(player);
    PhantomeBlessingHandler.clear(player);
    OursPolaireBlessingHandler.clear(player);
    LamaBlessingHandler.clear(player);
    ChevreBlessingHandler.clear(player);
    ChameauBlessingHandler.clear(player);
    if (player instanceof ServerPlayer serverPlayer) {
      ChauveSourisBlessingHandler.clear(serverPlayer);
      PoissonArgentBlessingHandler.clear(serverPlayer);
      VindicateurBlessingHandler.clear(serverPlayer);
      PillardBlessingHandler.clear(serverPlayer);
      EvocateurBlessingHandler.clear(serverPlayer);
      RavageurBlessingHandler.clear(serverPlayer);
      PerroquetBlessingHandler.clear(serverPlayer);
      GhastBlessingHandler.clear(serverPlayer);
      PiglinZombifieBlessingHandler.clear(serverPlayer);
      PiglinBlessingHandler.clear(serverPlayer);
      PiglinBruteBlessingHandler.clear(serverPlayer);
      ZoglinBlessingHandler.clear(serverPlayer);
      ArpenteurBlessingHandler.clear(serverPlayer);
      BlazeBlessingHandler.clear(serverPlayer);
      WitherSqueletteBlessingHandler.clear(serverPlayer);
      WitherSqueletteBossBlessingHandler.clear(serverPlayer);
      MagmaCubeBlessingHandler.clear(serverPlayer);
      GardienBlessingHandler.clear(serverPlayer);
      GrandGardienBlessingHandler.clear(serverPlayer);
      BreezeBlessingHandler.clear(serverPlayer);
      TatouBlessingHandler.clear(serverPlayer);
      RenifleurBlessingHandler.clear(serverPlayer);
      WardenBlessingHandler.clear(serverPlayer);
      EndermanBlessingHandler.clear(serverPlayer);
      ShulkerBlessingHandler.clear(serverPlayer);
      EndermiteBlessingHandler.clear(serverPlayer);
      AbeilleBlessingHandler.clear(serverPlayer);
      AllayBlessingHandler.clear(serverPlayer);
      VexBlessingHandler.clear(serverPlayer);
      PandaBlessingHandler.clear(serverPlayer);
      VacheChampignonBlessingHandler.clear(serverPlayer);
      GolemFerBlessingHandler.clear(serverPlayer);
      GolemNeigeBlessingHandler.clear(serverPlayer);
      VillageoisBlessingHandler.clear(serverPlayer);
      IllusionnisteBlessingHandler.clear(serverPlayer);
      EnderDragonBlessingHandler.clear(serverPlayer);
    }
    player.setData(ModAttachments.CHIMERE_BLESSING_STATE, ChimereBlessingState.DEFAULT);
  }
}
