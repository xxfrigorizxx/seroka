package com.seroka.chimere;

import java.util.List;

/**
 * Enregistrement des dieux Chimère (un par un, au fil des tests).
 */
public final class ChimereGodsBootstrap {

  private ChimereGodsBootstrap() {}

  public static void init() {
    registerGods();
  }

  private static void registerGods() {
    GodCatalog.register(new GodDefinition(
        GodIds.MORUE,
        1450,
        "blessing.seroka.god.morue"
    ));
    GodBlessingDescriptions.register(GodIds.MORUE, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.morue.effect.1",
        "blessing.seroka.god.morue.effect.2"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.SAUMON,
        1000,
        "blessing.seroka.god.saumon"
    ));
    GodBlessingDescriptions.register(GodIds.SAUMON, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.saumon.effect.1",
        "blessing.seroka.god.saumon.effect.2",
        "blessing.seroka.god.saumon.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.POISSON_TROPICAL,
        500,
        "blessing.seroka.god.poisson_tropical"
    ));
    GodBlessingDescriptions.register(GodIds.POISSON_TROPICAL, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.poisson_tropical.effect.1",
        "blessing.seroka.god.poisson_tropical.effect.2",
        "blessing.seroka.god.poisson_tropical.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.POISSON_GLOBE,
        250,
        "blessing.seroka.god.poisson_globe"
    ));
    GodBlessingDescriptions.register(GodIds.POISSON_GLOBE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.poisson_globe.effect.1",
        "blessing.seroka.god.poisson_globe.effect.2",
        "blessing.seroka.god.poisson_globe.effect.3",
        "blessing.seroka.god.poisson_globe.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.CALAMAR,
        750,
        "blessing.seroka.god.calamar"
    ));
    GodBlessingDescriptions.register(GodIds.CALAMAR, GodActivation.PASSIVE_MIDDLE_AND_F, List.of(
        "blessing.seroka.god.calamar.effect.1",
        "blessing.seroka.god.calamar.effect.2",
        "blessing.seroka.god.calamar.effect.3",
        "blessing.seroka.god.calamar.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.CALAMAR_LUMINESCENT,
        500,
        "blessing.seroka.god.calamar_luminescent"
    ));
    GodBlessingDescriptions.register(GodIds.CALAMAR_LUMINESCENT, GodActivation.PASSIVE_MIDDLE_AND_F, List.of(
        "blessing.seroka.god.calamar_luminescent.effect.1",
        "blessing.seroka.god.calamar_luminescent.effect.2",
        "blessing.seroka.god.calamar_luminescent.effect.3",
        "blessing.seroka.god.calamar_luminescent.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.TORTUE,
        250,
        "blessing.seroka.god.tortue"
    ));
    GodBlessingDescriptions.register(GodIds.TORTUE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.tortue.effect.1",
        "blessing.seroka.god.tortue.effect.2",
        "blessing.seroka.god.tortue.effect.3",
        "blessing.seroka.god.tortue.effect.4",
        "blessing.seroka.god.tortue.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.DAUPHIN,
        200,
        "blessing.seroka.god.dauphin"
    ));
    GodBlessingDescriptions.register(GodIds.DAUPHIN, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.dauphin.effect.1",
        "blessing.seroka.god.dauphin.effect.2",
        "blessing.seroka.god.dauphin.effect.3",
        "blessing.seroka.god.dauphin.effect.4",
        "blessing.seroka.god.dauphin.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.AXOLOTL,
        120,
        "blessing.seroka.god.axolotl"
    ));
    GodBlessingDescriptions.register(GodIds.AXOLOTL, GodActivation.PASSIVE_MIDDLE_AND_F, List.of(
        "blessing.seroka.god.axolotl.effect.1",
        "blessing.seroka.god.axolotl.effect.2",
        "blessing.seroka.god.axolotl.effect.3",
        "blessing.seroka.god.axolotl.effect.4",
        "blessing.seroka.god.axolotl.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.GRENOUILLE,
        1500,
        "blessing.seroka.god.grenouille"
    ));
    GodBlessingDescriptions.register(GodIds.GRENOUILLE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.grenouille.effect.1",
        "blessing.seroka.god.grenouille.effect.2",
        "blessing.seroka.god.grenouille.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.TETARD,
        250,
        "blessing.seroka.god.tetard"
    ));
    GodBlessingDescriptions.register(GodIds.TETARD, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.tetard.effect.1",
        "blessing.seroka.god.tetard.effect.2",
        "blessing.seroka.god.tetard.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ZOMBIE,
        2000,
        "blessing.seroka.god.zombie"
    ));
    GodBlessingDescriptions.register(GodIds.ZOMBIE, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.zombie.effect.1",
        "blessing.seroka.god.zombie.effect.2",
        "blessing.seroka.god.zombie.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ZOMBIE_VILLAGEOIS,
        714,
        "blessing.seroka.god.zombie_villageois"
    ));
    GodBlessingDescriptions.register(GodIds.ZOMBIE_VILLAGEOIS, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.zombie_villageois.effect.1",
        "blessing.seroka.god.zombie_villageois.effect.2",
        "blessing.seroka.god.zombie_villageois.effect.3",
        "blessing.seroka.god.zombie_villageois.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.HUSK,
        200,
        "blessing.seroka.god.husk"
    ));
    GodBlessingDescriptions.register(GodIds.HUSK, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.husk.effect.1",
        "blessing.seroka.god.husk.effect.2",
        "blessing.seroka.god.husk.effect.3",
        "blessing.seroka.god.husk.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.NOYE,
        110,
        "blessing.seroka.god.noye"
    ));
    GodBlessingDescriptions.register(GodIds.NOYE, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.noye.effect.1",
        "blessing.seroka.god.noye.effect.2",
        "blessing.seroka.god.noye.effect.3",
        "blessing.seroka.god.noye.effect.4",
        "blessing.seroka.god.noye.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.SQUELETTE,
        730,
        "blessing.seroka.god.squelette"
    ));
    GodBlessingDescriptions.register(GodIds.SQUELETTE, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.squelette.effect.1",
        "blessing.seroka.god.squelette.effect.2",
        "blessing.seroka.god.squelette.effect.3",
        "blessing.seroka.god.squelette.effect.4",
        "blessing.seroka.god.squelette.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.VAGABOND,
        140,
        "blessing.seroka.god.vagabond"
    ));
    GodBlessingDescriptions.register(GodIds.VAGABOND, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.vagabond.effect.1",
        "blessing.seroka.god.vagabond.effect.2",
        "blessing.seroka.god.vagabond.effect.3",
        "blessing.seroka.god.vagabond.effect.4",
        "blessing.seroka.god.vagabond.effect.5",
        "blessing.seroka.god.vagabond.effect.6"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.EMBOURBE,
        75,
        "blessing.seroka.god.embourbe"
    ));
    GodBlessingDescriptions.register(GodIds.EMBOURBE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.embourbe.effect.1",
        "blessing.seroka.god.embourbe.effect.2",
        "blessing.seroka.god.embourbe.effect.3",
        "blessing.seroka.god.embourbe.effect.4",
        "blessing.seroka.god.embourbe.effect.5",
        "blessing.seroka.god.embourbe.effect.6"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ARAIGNEE,
        575,
        "blessing.seroka.god.araignee"
    ));
    GodBlessingDescriptions.register(GodIds.ARAIGNEE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.araignee.effect.1",
        "blessing.seroka.god.araignee.effect.2",
        "blessing.seroka.god.araignee.effect.3",
        "blessing.seroka.god.araignee.effect.4",
        "blessing.seroka.god.araignee.effect.5",
        "blessing.seroka.god.araignee.effect.6"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ARAIGNEE_EMPOISONNEE,
        30,
        "blessing.seroka.god.araignee_empoisonnee"
    ));
    GodBlessingDescriptions.register(GodIds.ARAIGNEE_EMPOISONNEE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.araignee_empoisonnee.effect.1",
        "blessing.seroka.god.araignee_empoisonnee.effect.2",
        "blessing.seroka.god.araignee_empoisonnee.effect.3",
        "blessing.seroka.god.araignee_empoisonnee.effect.4",
        "blessing.seroka.god.araignee_empoisonnee.effect.5",
        "blessing.seroka.god.araignee_empoisonnee.effect.6",
        "blessing.seroka.god.araignee_empoisonnee.effect.7",
        "blessing.seroka.god.araignee_empoisonnee.effect.8"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.CREEPER,
        250,
        "blessing.seroka.god.creeper"
    ));
    GodBlessingDescriptions.register(GodIds.CREEPER, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.creeper.effect.1",
        "blessing.seroka.god.creeper.effect.2",
        "blessing.seroka.god.creeper.effect.3",
        "blessing.seroka.god.creeper.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.CREEPER_CHARGE,
        5,
        "blessing.seroka.god.creeper_charge"
    ));
    GodBlessingDescriptions.register(GodIds.CREEPER_CHARGE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.creeper_charge.effect.1",
        "blessing.seroka.god.creeper_charge.effect.2",
        "blessing.seroka.god.creeper_charge.effect.3",
        "blessing.seroka.god.creeper_charge.effect.4",
        "blessing.seroka.god.creeper_charge.effect.5",
        "blessing.seroka.god.creeper_charge.effect.6",
        "blessing.seroka.god.creeper_charge.effect.7"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.SORCIERE,
        240,
        "blessing.seroka.god.sorciere"
    ));
    GodBlessingDescriptions.register(GodIds.SORCIERE, GodActivation.MIDDLE_AND_F, List.of(
        "blessing.seroka.god.sorciere.effect.1",
        "blessing.seroka.god.sorciere.effect.2"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.PHANTOME,
        150,
        "blessing.seroka.god.phantome"
    ));
    GodBlessingDescriptions.register(GodIds.PHANTOME, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.phantome.effect.1",
        "blessing.seroka.god.phantome.effect.2",
        "blessing.seroka.god.phantome.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.OURS_POLAIRE,
        250,
        "blessing.seroka.god.ours_polaire"
    ));
    GodBlessingDescriptions.register(GodIds.OURS_POLAIRE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.ours_polaire.effect.1",
        "blessing.seroka.god.ours_polaire.effect.2",
        "blessing.seroka.god.ours_polaire.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.LAMA,
        100,
        "blessing.seroka.god.lama"
    ));
    GodBlessingDescriptions.register(GodIds.LAMA, GodActivation.PASSIVE_MIDDLE_AND_F, List.of(
        "blessing.seroka.god.lama.effect.1",
        "blessing.seroka.god.lama.effect.2",
        "blessing.seroka.god.lama.effect.3",
        "blessing.seroka.god.lama.effect.4",
        "blessing.seroka.god.lama.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.CHEVRE,
        275,
        "blessing.seroka.god.chevre"
    ));
    GodBlessingDescriptions.register(GodIds.CHEVRE, GodActivation.PASSIVE_MIDDLE_AND_F, List.of(
        "blessing.seroka.god.chevre.effect.1",
        "blessing.seroka.god.chevre.effect.2",
        "blessing.seroka.god.chevre.effect.3",
        "blessing.seroka.god.chevre.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.CHAMEAU,
        350,
        "blessing.seroka.god.chameau"
    ));
    GodBlessingDescriptions.register(GodIds.CHAMEAU, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.chameau.effect.1",
        "blessing.seroka.god.chameau.effect.2",
        "blessing.seroka.god.chameau.effect.3",
        "blessing.seroka.god.chameau.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.CHAUVE_SOURIS,
        20,
        "blessing.seroka.god.chauve_souris"
    ));
    GodBlessingDescriptions.register(GodIds.CHAUVE_SOURIS, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.chauve_souris.effect.1",
        "blessing.seroka.god.chauve_souris.effect.2",
        "blessing.seroka.god.chauve_souris.effect.3",
        "blessing.seroka.god.chauve_souris.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.POISSON_ARGENT,
        450,
        "blessing.seroka.god.poisson_argent"
    ));
    GodBlessingDescriptions.register(GodIds.POISSON_ARGENT, GodActivation.MIDDLE_CLICK, List.of(
        "blessing.seroka.god.poisson_argent.effect.1",
        "blessing.seroka.god.poisson_argent.effect.2",
        "blessing.seroka.god.poisson_argent.effect.3",
        "blessing.seroka.god.poisson_argent.effect.4",
        "blessing.seroka.god.poisson_argent.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.VINDICATEUR,
        235,
        "blessing.seroka.god.vindicateur"
    ));
    GodBlessingDescriptions.register(GodIds.VINDICATEUR, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.vindicateur.effect.1",
        "blessing.seroka.god.vindicateur.effect.2",
        "blessing.seroka.god.vindicateur.effect.3",
        "blessing.seroka.god.vindicateur.effect.4",
        "blessing.seroka.god.vindicateur.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.PILLARD,
        515,
        "blessing.seroka.god.pillard"
    ));
    GodBlessingDescriptions.register(GodIds.PILLARD, GodActivation.PASSIVE_MIDDLE_AND_F, List.of(
        "blessing.seroka.god.pillard.effect.1",
        "blessing.seroka.god.pillard.effect.2",
        "blessing.seroka.god.pillard.effect.3",
        "blessing.seroka.god.pillard.effect.4",
        "blessing.seroka.god.pillard.effect.5",
        "blessing.seroka.god.pillard.effect.6"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.EVOCATEUR,
        14,
        "blessing.seroka.god.evocateur"
    ));
    GodBlessingDescriptions.register(GodIds.EVOCATEUR, GodActivation.PASSIVE_MIDDLE_F_AND_H, List.of(
        "blessing.seroka.god.evocateur.effect.1",
        "blessing.seroka.god.evocateur.effect.2",
        "blessing.seroka.god.evocateur.effect.3",
        "blessing.seroka.god.evocateur.effect.4",
        "blessing.seroka.god.evocateur.effect.5",
        "blessing.seroka.god.evocateur.effect.6",
        "blessing.seroka.god.evocateur.effect.7"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.RAVAGEUR,
        7,
        "blessing.seroka.god.ravageur"
    ));
    GodBlessingDescriptions.register(GodIds.RAVAGEUR, GodActivation.PASSIVE_MIDDLE_AND_F, List.of(
        "blessing.seroka.god.ravageur.effect.1",
        "blessing.seroka.god.ravageur.effect.2",
        "blessing.seroka.god.ravageur.effect.3",
        "blessing.seroka.god.ravageur.effect.4",
        "blessing.seroka.god.ravageur.effect.5",
        "blessing.seroka.god.ravageur.effect.6"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.PERROQUET,
        673,
        "blessing.seroka.god.perroquet"
    ));
    GodBlessingDescriptions.register(GodIds.PERROQUET, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.perroquet.effect.1",
        "blessing.seroka.god.perroquet.effect.2",
        "blessing.seroka.god.perroquet.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.GHAST,
        75,
        "blessing.seroka.god.ghast"
    ));
    GodBlessingDescriptions.register(GodIds.GHAST, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.ghast.effect.1",
        "blessing.seroka.god.ghast.effect.2",
        "blessing.seroka.god.ghast.effect.3",
        "blessing.seroka.god.ghast.effect.4",
        "blessing.seroka.god.ghast.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.PIGLIN_ZOMBIFIE,
        734,
        "blessing.seroka.god.piglin_zombifie"
    ));
    GodBlessingDescriptions.register(GodIds.PIGLIN_ZOMBIFIE, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.piglin_zombifie.effect.1",
        "blessing.seroka.god.piglin_zombifie.effect.2",
        "blessing.seroka.god.piglin_zombifie.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.PIGLIN,
        837,
        "blessing.seroka.god.piglin"
    ));
    GodBlessingDescriptions.register(GodIds.PIGLIN, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.piglin.effect.1",
        "blessing.seroka.god.piglin.effect.2",
        "blessing.seroka.god.piglin.effect.3",
        "blessing.seroka.god.piglin.effect.4",
        "blessing.seroka.god.piglin.effect.5",
        "blessing.seroka.god.piglin.effect.6"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.PIGLIN_BRUTE,
        33,
        "blessing.seroka.god.piglin_brute"
    ));
    GodBlessingDescriptions.register(GodIds.PIGLIN_BRUTE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.piglin_brute.effect.1",
        "blessing.seroka.god.piglin_brute.effect.2",
        "blessing.seroka.god.piglin_brute.effect.3",
        "blessing.seroka.god.piglin_brute.effect.4",
        "blessing.seroka.god.piglin_brute.effect.5",
        "blessing.seroka.god.piglin_brute.effect.6"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ZOGLIN,
        750,
        "blessing.seroka.god.zoglin"
    ));
    GodBlessingDescriptions.register(GodIds.ZOGLIN, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.zoglin.effect.1",
        "blessing.seroka.god.zoglin.effect.2",
        "blessing.seroka.god.zoglin.effect.3",
        "blessing.seroka.god.zoglin.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ARPENTEUR,
        954,
        "blessing.seroka.god.arpenteur"
    ));
    GodBlessingDescriptions.register(GodIds.ARPENTEUR, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.arpenteur.effect.1",
        "blessing.seroka.god.arpenteur.effect.2",
        "blessing.seroka.god.arpenteur.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.BLAZE,
        210,
        "blessing.seroka.god.blaze"
    ));
    GodBlessingDescriptions.register(GodIds.BLAZE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.blaze.effect.1",
        "blessing.seroka.god.blaze.effect.2",
        "blessing.seroka.god.blaze.effect.3",
        "blessing.seroka.god.blaze.effect.4",
        "blessing.seroka.god.blaze.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.WITHER_SQUELETTE,
        121,
        "blessing.seroka.god.wither_squelette"
    ));
    GodBlessingDescriptions.register(GodIds.WITHER_SQUELETTE, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.wither_squelette.effect.1",
        "blessing.seroka.god.wither_squelette.effect.2",
        "blessing.seroka.god.wither_squelette.effect.3",
        "blessing.seroka.god.wither_squelette.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.WITHER_SQUELETTE_BOSS,
        1,
        "blessing.seroka.god.wither_squelette_boss"
    ));
    GodBlessingDescriptions.register(GodIds.WITHER_SQUELETTE_BOSS, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.wither_squelette_boss.effect.1",
        "blessing.seroka.god.wither_squelette_boss.effect.2",
        "blessing.seroka.god.wither_squelette_boss.effect.3",
        "blessing.seroka.god.wither_squelette_boss.effect.4",
        "blessing.seroka.god.wither_squelette_boss.effect.5",
        "blessing.seroka.god.wither_squelette_boss.effect.6",
        "blessing.seroka.god.wither_squelette_boss.effect.7"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.MAGMA_CUBE,
        126,
        "blessing.seroka.god.magma_cube"
    ));
    GodBlessingDescriptions.register(GodIds.MAGMA_CUBE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.magma_cube.effect.1",
        "blessing.seroka.god.magma_cube.effect.2",
        "blessing.seroka.god.magma_cube.effect.3",
        "blessing.seroka.god.magma_cube.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.GARDIEN,
        582,
        "blessing.seroka.god.gardien"
    ));
    GodBlessingDescriptions.register(GodIds.GARDIEN, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.gardien.effect.1",
        "blessing.seroka.god.gardien.effect.2",
        "blessing.seroka.god.gardien.effect.3",
        "blessing.seroka.god.gardien.effect.4",
        "blessing.seroka.god.gardien.effect.5",
        "blessing.seroka.god.gardien.effect.6"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.GRAND_GARDIEN,
        1,
        "blessing.seroka.god.grand_gardien"
    ));
    GodBlessingDescriptions.register(GodIds.GRAND_GARDIEN, GodActivation.PASSIVE_MIDDLE_AND_F, List.of(
        "blessing.seroka.god.grand_gardien.effect.1",
        "blessing.seroka.god.grand_gardien.effect.2",
        "blessing.seroka.god.grand_gardien.effect.3",
        "blessing.seroka.god.grand_gardien.effect.4",
        "blessing.seroka.god.grand_gardien.effect.5",
        "blessing.seroka.god.grand_gardien.effect.6",
        "blessing.seroka.god.grand_gardien.effect.7",
        "blessing.seroka.god.grand_gardien.effect.8"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.BREEZE,
        250,
        "blessing.seroka.god.breeze"
    ));
    GodBlessingDescriptions.register(GodIds.BREEZE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.breeze.effect.1",
        "blessing.seroka.god.breeze.effect.2",
        "blessing.seroka.god.breeze.effect.3",
        "blessing.seroka.god.breeze.effect.4",
        "blessing.seroka.god.breeze.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.SLIME,
        1243,
        "blessing.seroka.god.slime"
    ));
    GodBlessingDescriptions.register(GodIds.SLIME, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.slime.effect.1",
        "blessing.seroka.god.slime.effect.2",
        "blessing.seroka.god.slime.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.TATOU,
        1720,
        "blessing.seroka.god.tatou"
    ));
    GodBlessingDescriptions.register(GodIds.TATOU, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.tatou.effect.1",
        "blessing.seroka.god.tatou.effect.2",
        "blessing.seroka.god.tatou.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.RENIFLEUR,
        100,
        "blessing.seroka.god.renifleur"
    ));
    GodBlessingDescriptions.register(GodIds.RENIFLEUR, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.renifleur.effect.1",
        "blessing.seroka.god.renifleur.effect.2",
        "blessing.seroka.god.renifleur.effect.3",
        "blessing.seroka.god.renifleur.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.WARDEN,
        2,
        "blessing.seroka.god.warden"
    ));
    GodBlessingDescriptions.register(GodIds.WARDEN, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.warden.effect.1",
        "blessing.seroka.god.warden.effect.2",
        "blessing.seroka.god.warden.effect.3",
        "blessing.seroka.god.warden.effect.4",
        "blessing.seroka.god.warden.effect.5",
        "blessing.seroka.god.warden.effect.6"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ENDERMAN,
        67,
        "blessing.seroka.god.enderman"
    ));
    GodBlessingDescriptions.register(GodIds.ENDERMAN, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.enderman.effect.1",
        "blessing.seroka.god.enderman.effect.2",
        "blessing.seroka.god.enderman.effect.3",
        "blessing.seroka.god.enderman.effect.4",
        "blessing.seroka.god.enderman.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.SHULKER,
        285,
        "blessing.seroka.god.shulker"
    ));
    GodBlessingDescriptions.register(GodIds.SHULKER, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.shulker.effect.1",
        "blessing.seroka.god.shulker.effect.2",
        "blessing.seroka.god.shulker.effect.3",
        "blessing.seroka.god.shulker.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ENDERMITE,
        250,
        "blessing.seroka.god.endermite"
    ));
    GodBlessingDescriptions.register(GodIds.ENDERMITE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.endermite.effect.1",
        "blessing.seroka.god.endermite.effect.2",
        "blessing.seroka.god.endermite.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ABEILLE,
        300,
        "blessing.seroka.god.abeille"
    ));
    GodBlessingDescriptions.register(GodIds.ABEILLE, GodActivation.PASSIVE_MIDDLE_AND_F, List.of(
        "blessing.seroka.god.abeille.effect.1",
        "blessing.seroka.god.abeille.effect.2",
        "blessing.seroka.god.abeille.effect.3",
        "blessing.seroka.god.abeille.effect.4",
        "blessing.seroka.god.abeille.effect.5",
        "blessing.seroka.god.abeille.effect.6"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ALLAY,
        360,
        "blessing.seroka.god.allay"
    ));
    GodBlessingDescriptions.register(GodIds.ALLAY, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.allay.effect.1"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.VEX,
        340,
        "blessing.seroka.god.vex"
    ));
    GodBlessingDescriptions.register(GodIds.VEX, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.vex.effect.1",
        "blessing.seroka.god.vex.effect.2",
        "blessing.seroka.god.vex.effect.3",
        "blessing.seroka.god.vex.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.PANDA,
        1600,
        "blessing.seroka.god.panda"
    ));
    GodBlessingDescriptions.register(GodIds.PANDA, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.panda.effect.1",
        "blessing.seroka.god.panda.effect.2",
        "blessing.seroka.god.panda.effect.3",
        "blessing.seroka.god.panda.effect.4",
        "blessing.seroka.god.panda.effect.5",
        "blessing.seroka.god.panda.effect.6"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.VACHE_CHAMPIGNON,
        1,
        "blessing.seroka.god.vache_champignon"
    ));
    GodBlessingDescriptions.register(GodIds.VACHE_CHAMPIGNON, GodActivation.PASSIVE_MIDDLE_AND_F, List.of(
        "blessing.seroka.god.vache_champignon.effect.1",
        "blessing.seroka.god.vache_champignon.effect.2",
        "blessing.seroka.god.vache_champignon.effect.3",
        "blessing.seroka.god.vache_champignon.effect.4",
        "blessing.seroka.god.vache_champignon.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.GOLEM_FER,
        204,
        "blessing.seroka.god.golem_fer"
    ));
    GodBlessingDescriptions.register(GodIds.GOLEM_FER, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.golem_fer.effect.1",
        "blessing.seroka.god.golem_fer.effect.2",
        "blessing.seroka.god.golem_fer.effect.3",
        "blessing.seroka.god.golem_fer.effect.4",
        "blessing.seroka.god.golem_fer.effect.5"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.GOLEM_NEIGE,
        360,
        "blessing.seroka.god.golem_neige"
    ));
    GodBlessingDescriptions.register(GodIds.GOLEM_NEIGE, GodActivation.MIDDLE_CLICK, List.of(
        "blessing.seroka.god.golem_neige.effect.1"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.VILLAGEOIS,
        250,
        "blessing.seroka.god.villageois"
    ));
    GodBlessingDescriptions.register(GodIds.VILLAGEOIS, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.villageois.effect.1",
        "blessing.seroka.god.villageois.effect.2"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.MARCHAND_AMBULANT,
        10,
        "blessing.seroka.god.marchand_ambulant"
    ));
    GodBlessingDescriptions.register(GodIds.MARCHAND_AMBULANT, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.marchand_ambulant.effect.1",
        "blessing.seroka.god.marchand_ambulant.effect.2"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ILLUSIONNISTE,
        5,
        "blessing.seroka.god.illusionniste"
    ));
    GodBlessingDescriptions.register(GodIds.ILLUSIONNISTE, GodActivation.PASSIVE_AND_F, List.of(
        "blessing.seroka.god.illusionniste.effect.1",
        "blessing.seroka.god.illusionniste.effect.2",
        "blessing.seroka.god.illusionniste.effect.3",
        "blessing.seroka.god.illusionniste.effect.4"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ENDER_DRAGON,
        1,
        "blessing.seroka.god.ender_dragon"
    ));
    GodBlessingDescriptions.register(GodIds.ENDER_DRAGON, GodActivation.PASSIVE_MIDDLE_AND_F, List.of(
        "blessing.seroka.god.ender_dragon.effect.1",
        "blessing.seroka.god.ender_dragon.effect.2",
        "blessing.seroka.god.ender_dragon.effect.3",
        "blessing.seroka.god.ender_dragon.effect.4",
        "blessing.seroka.god.ender_dragon.effect.5",
        "blessing.seroka.god.ender_dragon.effect.6",
        "blessing.seroka.god.ender_dragon.effect.7",
        "blessing.seroka.god.ender_dragon.effect.8",
        "blessing.seroka.god.ender_dragon.effect.9"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.VACHE,
        1500,
        "blessing.seroka.god.vache"
    ));
    GodBlessingDescriptions.register(GodIds.VACHE, List.of(
        "blessing.seroka.god.vache.effect.1"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.MOUTON,
        500,
        "blessing.seroka.god.mouton"
    ));
    GodBlessingDescriptions.register(GodIds.MOUTON, List.of(
        "blessing.seroka.god.mouton.effect.1",
        "blessing.seroka.god.mouton.effect.2"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.COCHON,
        1200,
        "blessing.seroka.god.cochon"
    ));
    GodBlessingDescriptions.register(GodIds.COCHON, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.cochon.effect.1",
        "blessing.seroka.god.cochon.effect.2",
        "blessing.seroka.god.cochon.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.POULET,
        1000,
        "blessing.seroka.god.poulet"
    ));
    GodBlessingDescriptions.register(GodIds.POULET, List.of(
        "blessing.seroka.god.poulet.effect.1",
        "blessing.seroka.god.poulet.effect.2"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.LAPIN,
        900,
        "blessing.seroka.god.lapin"
    ));
    GodBlessingDescriptions.register(GodIds.LAPIN, List.of(
        "blessing.seroka.god.lapin.effect.1",
        "blessing.seroka.god.lapin.effect.2"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.CHEVAL,
        800,
        "blessing.seroka.god.cheval"
    ));
    GodBlessingDescriptions.register(GodIds.CHEVAL, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.cheval.effect.1",
        "blessing.seroka.god.cheval.effect.2",
        "blessing.seroka.god.cheval.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.ANE,
        850,
        "blessing.seroka.god.ane"
    ));
    GodBlessingDescriptions.register(GodIds.ANE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.ane.effect.1",
        "blessing.seroka.god.ane.effect.2",
        "blessing.seroka.god.ane.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.MULE,
        150,
        "blessing.seroka.god.mule"
    ));
    GodBlessingDescriptions.register(GodIds.MULE, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.mule.effect.1",
        "blessing.seroka.god.mule.effect.2",
        "blessing.seroka.god.mule.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.LOUP,
        750,
        "blessing.seroka.god.loup"
    ));
    GodBlessingDescriptions.register(GodIds.LOUP, GodActivation.PASSIVE, List.of(
        "blessing.seroka.god.loup.effect.1",
        "blessing.seroka.god.loup.effect.2",
        "blessing.seroka.god.loup.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.RENARD,
        750,
        "blessing.seroka.god.renard"
    ));
    GodBlessingDescriptions.register(GodIds.RENARD, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.renard.effect.1",
        "blessing.seroka.god.renard.effect.2",
        "blessing.seroka.god.renard.effect.3"
    ));

    GodCatalog.register(new GodDefinition(
        GodIds.LEFIN,
        250,
        "blessing.seroka.god.lefin"
    ));
    GodBlessingDescriptions.register(GodIds.LEFIN, GodActivation.PASSIVE_AND_MIDDLE, List.of(
        "blessing.seroka.god.lefin.effect.1",
        "blessing.seroka.god.lefin.effect.2",
        "blessing.seroka.god.lefin.effect.3",
        "blessing.seroka.god.lefin.effect.4"
    ));
  }
}
