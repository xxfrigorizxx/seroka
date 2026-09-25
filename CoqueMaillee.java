package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.block.CanonBlock;
import com.seroka.navire.NavireEntity;
import com.seroka.navire.StructureNavire;
import com.seroka.navire.VoileNavire;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Coque cousue par tranches, puis rejouée dans le tuyau des entités.
 *
 * <p>Dessiner chaque planche comme un bloc du monde, à chaque image, faisait tout lâcher dès qu'un
 * trois-ponts entrait dans le champ : le modèle était reloué, les faces enfouies recalculées, et le
 * processeur n'en pouvait plus. Ici chaque tranche de seize blocs est cousue une bonne fois — faces
 * murées exclues, lumière déjà prise — puis seulement recopiée dans le même tampon que le reste des
 * entités. C'est ce tampon-là qui place la coque dans le monde : on ne touche pas aux shaders de
 * chunks, qui avaient collé le navire au regard ou l'avaient rendu invisible.
 *
 * <p>Une tranche n'est recousue que si l'un de ses blocs change. La lumière, elle, se rafraîchit sur
 * les faces déjà cousues, sans tout recommencer quand le navire dérive.
 */
final class CoqueMaillee {

  /** Côté d'une tranche, en blocs. */
  private static final int COTE = 16;

  /** Demi-diagonale d'une tranche : marge à accorder aux tests de visibilité. */
  private static final double RAYON_TRANCHE = COTE * Math.sqrt(3.0D) / 2.0D;

  /** Tranches cousues en même temps en arrière-plan. */
  private static final int CHANTIERS_MAX = 6;

  /** Blocs dont on recalcule la lumière par image, tous navires confondus pour cette coque. */
  private static final int LUMIERES_PAR_IMAGE = 256;

  /** Blocs de dérive tolérés avant de reprendre l'éclairage. */
  private static final int DERIVE_LUMIERE = 8;

  /** Cosinus de l'angle au-delà duquel une tranche est jugée hors du champ de vision. */
  private static final double COSINUS_CHAMP = -0.35D;

  /** Ordre de passage des calques : le translucide en dernier, comme partout ailleurs. */
  private static final List<RenderType> CALQUES = List.of(
      RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout(), RenderType.translucent());

  private final Map<Long, Tranche> tranches = new LinkedHashMap<>();

  private int versionMaillee = -1;
  private int blocsConnus = -1;
  private boolean toutARefaire = true;
  private int curseurLumiere;
  @Nullable
  private BlockPos ancrageLumiere;

  /**
   * Met le maillage à jour : ce qui a changé est recousu, le reste est laissé tranquille.
   *
   * @param oeil position de la caméra dans le repère de la coque, pour servir le plus près d'abord
   */
  void preparer(NavireEntity navire, BlockRenderDispatcher rendu, BlockColors couleurs, Vec3 oeil) {
    StructureNavire structure = navire.structure();
    int version = structure.version();
    int blocs = structure.nombreDeBlocs();
    if (version != versionMaillee || blocs != blocsConnus) {
      noterLesChangements(structure);
      versionMaillee = version;
      blocsConnus = blocs;
    }
    noterLaDerive(navire);
    if (toutARefaire) {
      recenserLesTranches(structure);
      toutARefaire = false;
    }
    lancerLesChantiers(navire, rendu, couleurs, oeil);
    recolterLesChantiers();
    rafraichirEclairage(navire);
  }

  /** Recopie les tranches visibles dans le tampon d'entités, un calque à la fois. */
  void dessiner(PoseStack pile, MultiBufferSource buffer, Vec3 oeil, Vec3 regard, double portee) {
    if (tranches.isEmpty()) {
      return;
    }
    for (RenderType calque : CALQUES) {
      VertexConsumer tampon = null;
      for (Tranche tranche : tranches.values()) {
        List<FaceCuite> faces = tranche.calques.get(calque);
        if (faces == null || faces.isEmpty() || !tranche.enVue(oeil, regard, portee)) {
          continue;
        }
        if (tampon == null) {
          tampon = buffer.getBuffer(calque);
        }
        recopier(pile, tampon, faces);
      }
    }
    for (Tranche tranche : tranches.values()) {
      if (!tranche.enVue(oeil, regard, portee)) {
        continue;
      }
      for (Map.Entry<RenderType, List<FaceCuite>> entree : tranche.calques.entrySet()) {
        if (CALQUES.contains(entree.getKey()) || entree.getValue().isEmpty()) {
          continue;
        }
        recopier(pile, buffer.getBuffer(entree.getKey()), entree.getValue());
      }
    }
  }

  private static void recopier(PoseStack pile, VertexConsumer tampon, List<FaceCuite> faces) {
    for (FaceCuite face : faces) {
      pile.pushPose();
      pile.translate(face.bloc.x, face.bloc.y, face.bloc.z);
      tampon.putBulkData(
          pile.last(),
          face.quad,
          face.rouge,
          face.vert,
          face.bleu,
          1.0F,
          face.bloc.lumiere,
          OverlayTexture.NO_OVERLAY);
      pile.popPose();
    }
  }

  // ------------------------------------------------------------------ entretien

  private void noterLesChangements(StructureNavire structure) {
    List<BlockPos> changements = structure.changementsDepuis(versionMaillee);
    if (changements == null) {
      toutARefaire = true;
      return;
    }
    for (BlockPos offset : changements) {
      tranches.computeIfAbsent(cle(offset.getX(), offset.getY(), offset.getZ()),
          code -> nouvelleTranche(offset.getX(), offset.getY(), offset.getZ())).sale = true;
      for (Direction face : Direction.values()) {
        Tranche mitoyenne = tranches.get(cle(
            offset.getX() + face.getStepX(),
            offset.getY() + face.getStepY(),
            offset.getZ() + face.getStepZ()));
        if (mitoyenne != null) {
          mitoyenne.sale = true;
        }
      }
    }
  }

  private void noterLaDerive(NavireEntity navire) {
    BlockPos ancrage = navire.blockPosition();
    if (ancrageLumiere != null
        && ancrageLumiere.distSqr(ancrage) < DERIVE_LUMIERE * DERIVE_LUMIERE) {
      return;
    }
    ancrageLumiere = ancrage;
    curseurLumiere = 0;
  }

  private void recenserLesTranches(StructureNavire structure) {
    tranches.clear();
    structure.parcourir((x, y, z, etat) -> tranches.computeIfAbsent(cle(x, y, z),
        code -> nouvelleTranche(x, y, z)));
  }

  private void lancerLesChantiers(NavireEntity navire, BlockRenderDispatcher rendu,
      BlockColors couleurs, Vec3 oeil) {
    int enCours = 0;
    for (Tranche tranche : tranches.values()) {
      if (tranche.chantier != null) {
        enCours++;
      }
    }
    while (enCours < CHANTIERS_MAX) {
      Tranche tranche = prochaineTranche(oeil);
      if (tranche == null) {
        return;
      }
      Instantane cliche = releverInstantane(navire, tranche);
      tranche.sale = false;
      tranche.chantier = CompletableFuture.supplyAsync(
          () -> coudre(cliche, rendu, couleurs), Util.backgroundExecutor());
      enCours++;
    }
  }

  @Nullable
  private Tranche prochaineTranche(Vec3 oeil) {
    Tranche choisie = null;
    double meilleure = Double.MAX_VALUE;
    for (Tranche tranche : tranches.values()) {
      if (!tranche.sale || tranche.chantier != null) {
        continue;
      }
      double distance = tranche.distanceCarree(oeil);
      if (distance < meilleure) {
        meilleure = distance;
        choisie = tranche;
      }
    }
    return choisie;
  }

  private void recolterLesChantiers() {
    for (Tranche tranche : tranches.values()) {
      CompletableFuture<Maille> chantier = tranche.chantier;
      if (chantier == null || !chantier.isDone()) {
        continue;
      }
      tranche.chantier = null;
      Maille maille = chantier.join();
      if (maille != null) {
        tranche.adopter(maille);
      }
    }
  }

  private void rafraichirEclairage(NavireEntity navire) {
    if (tranches.isEmpty()) {
      return;
    }
    List<Tranche> liste = new ArrayList<>(tranches.values());
    int restantes = LUMIERES_PAR_IMAGE;
    int index = Math.floorMod(curseurLumiere, liste.size());
    int vues = 0;
    while (restantes > 0 && vues < liste.size()) {
      restantes = liste.get(index).rafraichirEclairage(navire, restantes);
      index = (index + 1) % liste.size();
      vues++;
    }
    curseurLumiere = index;
  }

  // -------------------------------------------------------------------- couture

  private record Instantane(int coinX, int coinY, int coinZ, BlockState[] etats, int[] lumieres,
      boolean[] grees) {

    private static final int BORDE = COTE + 2;

    BlockState etat(int dx, int dy, int dz) {
      return etats[((dy + 1) * BORDE + dz + 1) * BORDE + dx + 1];
    }

    int lumiere(int dx, int dy, int dz) {
      return lumieres[(dy * COTE + dz) * COTE + dx];
    }

    /** Vrai si cette case appartient à une voile : le maillage la laisse au rendu du gréement. */
    boolean gree(int dx, int dy, int dz) {
      return grees[((dy + 1) * BORDE + dz + 1) * BORDE + dx + 1];
    }
  }

  private static Instantane releverInstantane(NavireEntity navire, Tranche tranche) {
    StructureNavire structure = navire.structure();
    BlockState[] etats = new BlockState[Instantane.BORDE * Instantane.BORDE * Instantane.BORDE];
    int[] lumieres = new int[COTE * COTE * COTE];
    boolean[] grees = new boolean[Instantane.BORDE * Instantane.BORDE * Instantane.BORDE];
    for (int dy = -1; dy <= COTE; dy++) {
      for (int dz = -1; dz <= COTE; dz++) {
        for (int dx = -1; dx <= COTE; dx++) {
          etats[((dy + 1) * Instantane.BORDE + dz + 1) * Instantane.BORDE + dx + 1] =
              structure.bloc(tranche.coinX + dx, tranche.coinY + dy, tranche.coinZ + dz);
        }
      }
    }
    marquerLeGreement(navire, tranche, grees);
    BlockPos.MutableBlockPos curseur = new BlockPos.MutableBlockPos();
    for (int dy = 0; dy < COTE; dy++) {
      for (int dz = 0; dz < COTE; dz++) {
        for (int dx = 0; dx < COTE; dx++) {
          int index = ((dy + 1) * Instantane.BORDE + dz + 1) * Instantane.BORDE + dx + 1;
          if (etats[index].isAir()) {
            continue;
          }
          curseur.set(tranche.coinX + dx, tranche.coinY + dy, tranche.coinZ + dz);
          lumieres[(dy * COTE + dz) * COTE + dx] =
              LevelRenderer.getLightColor(navire.level(), navire.positionMonde(curseur));
        }
      }
    }
    return new Instantane(tranche.coinX, tranche.coinY, tranche.coinZ, etats, lumieres, grees);
  }

  /**
   * Note les cases que le gréement dessine lui-même : la toile et la vergue du bas.
   *
   * <p>La vergue du haut reste dans le maillage, elle ne bouge jamais. Le voisinage est marqué lui
   * aussi : une voile ne doit pas murer ce qu'elle touche, sinon la vergue du haut perdrait son
   * dessous et le mât ses flancs dès que la toile se replie.
   *
   * <p>Le marquage se fait ici, sur le fil principal, parce que la couture part ensuite en tâche de
   * fond et ne doit plus rien demander au navire.
   */
  private static void marquerLeGreement(NavireEntity navire, Tranche tranche, boolean[] grees) {
    Map<BlockPos, VoileNavire> voiles = navire.voiles();
    if (voiles.isEmpty()) {
      return;
    }
    // On balaie les voiles, pas le volume de la tranche : une voile ne couvre qu'une surface.
    for (VoileNavire voile : voiles.values()) {
      for (int longAxe = voile.debut(); longAxe <= voile.fin(); longAxe++) {
        // Depuis la vergue qui chausse la colonne : elle monte avec la toile, donc elle est mobile.
        for (int y = voile.basDeColonne(longAxe) - 1; y < voile.vergueHaut(); y++) {
          BlockPos place = voile.offset(longAxe, y);
          int dx = place.getX() - tranche.coinX;
          int dy = place.getY() - tranche.coinY;
          int dz = place.getZ() - tranche.coinZ;
          if (dx < -1 || dx > COTE || dy < -1 || dy > COTE || dz < -1 || dz > COTE) {
            continue;
          }
          grees[((dy + 1) * Instantane.BORDE + dz + 1) * Instantane.BORDE + dx + 1] = true;
        }
      }
    }
  }

  private record Maille(Map<RenderType, List<FaceCuite>> calques, List<BlocCuit> blocs) {}

  @Nullable
  private static Maille coudre(Instantane cliche, BlockRenderDispatcher rendu,
      BlockColors couleurs) {
    Map<RenderType, List<FaceCuite>> calques = new LinkedHashMap<>();
    List<BlocCuit> blocs = new ArrayList<>();
    RandomSource des = RandomSource.create();

    for (int dy = 0; dy < COTE; dy++) {
      for (int dz = 0; dz < COTE; dz++) {
        for (int dx = 0; dx < COTE; dx++) {
          BlockState etat = cliche.etat(dx, dy, dz);
          if (etat.isAir() || etat.getRenderShape() != RenderShape.MODEL) {
            continue;
          }
          if (etat.getBlock() instanceof CanonBlock || cliche.gree(dx, dy, dz)) {
            continue;
          }
          BakedModel modele = rendu.getBlockModel(etat);
          int lumiere = cliche.lumiere(dx, dy, dz);
          BlocCuit bloc = new BlocCuit(cliche.coinX + dx, cliche.coinY + dy, cliche.coinZ + dz,
              lumiere);
          boolean auMoinsUneFace = false;
          for (RenderType calque : modele.getRenderTypes(etat, des, ModelData.EMPTY)) {
            List<FaceCuite> faces = calques.computeIfAbsent(calque, type -> new ArrayList<>());
            auMoinsUneFace |= coudreUnBloc(cliche, dx, dy, dz, etat, modele, calque, faces, bloc,
                des, couleurs);
          }
          if (auMoinsUneFace) {
            blocs.add(bloc);
          }
        }
      }
    }
    return new Maille(calques, blocs);
  }

  private static boolean coudreUnBloc(Instantane cliche, int dx, int dy, int dz, BlockState etat,
      BakedModel modele, RenderType calque, List<FaceCuite> faces, BlocCuit bloc,
      RandomSource des, BlockColors couleurs) {
    boolean pose = false;
    for (Direction face : Direction.values()) {
      if (muree(cliche, dx, dy, dz, face)) {
        continue;
      }
      des.setSeed(42L);
      pose |= poserDesQuads(modele.getQuads(etat, face, des, ModelData.EMPTY, calque), faces, bloc,
          etat, couleurs);
    }
    des.setSeed(42L);
    pose |= poserDesQuads(modele.getQuads(etat, null, des, ModelData.EMPTY, calque), faces, bloc,
        etat, couleurs);
    return pose;
  }

  private static boolean muree(Instantane cliche, int dx, int dy, int dz, Direction face) {
    int vx = dx + face.getStepX();
    int vy = dy + face.getStepY();
    int vz = dz + face.getStepZ();
    if (cliche.gree(vx, vy, vz)) {
      // Une voile s'écarte de sa place : elle ne peut pas servir de mur à son voisin.
      return false;
    }
    BlockState voisin = cliche.etat(vx, vy, vz);
    return !voisin.isAir() && voisin.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
  }

  private static boolean poserDesQuads(List<BakedQuad> quads, List<FaceCuite> faces, BlocCuit bloc,
      BlockState etat, BlockColors couleurs) {
    if (quads.isEmpty()) {
      return false;
    }
    for (BakedQuad quad : quads) {
      float rouge = 1.0F;
      float vert = 1.0F;
      float bleu = 1.0F;
      if (quad.isTinted()) {
        int teinte = couleurs.getColor(etat, null, null, quad.getTintIndex());
        rouge = (teinte >> 16 & 0xFF) / 255.0F;
        vert = (teinte >> 8 & 0xFF) / 255.0F;
        bleu = (teinte & 0xFF) / 255.0F;
      }
      faces.add(new FaceCuite(bloc, quad, rouge, vert, bleu));
    }
    return true;
  }

  // -------------------------------------------------------------------- tranches

  private static long cle(int x, int y, int z) {
    return BlockPos.asLong(Math.floorDiv(x, COTE), Math.floorDiv(y, COTE), Math.floorDiv(z, COTE));
  }

  private static Tranche nouvelleTranche(int x, int y, int z) {
    return new Tranche(
        Math.floorDiv(x, COTE) * COTE,
        Math.floorDiv(y, COTE) * COTE,
        Math.floorDiv(z, COTE) * COTE);
  }

  private static final class FaceCuite {
    final BlocCuit bloc;
    final BakedQuad quad;
    final float rouge;
    final float vert;
    final float bleu;

    FaceCuite(BlocCuit bloc, BakedQuad quad, float rouge, float vert, float bleu) {
      this.bloc = bloc;
      this.quad = quad;
      this.rouge = rouge;
      this.vert = vert;
      this.bleu = bleu;
    }
  }

  private static final class BlocCuit {
    final int x;
    final int y;
    final int z;
    int lumiere;

    BlocCuit(int x, int y, int z, int lumiere) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.lumiere = lumiere;
    }
  }

  private static final class Tranche {
    final int coinX;
    final int coinY;
    final int coinZ;
    final Map<RenderType, List<FaceCuite>> calques = new LinkedHashMap<>();
    final List<BlocCuit> blocs = new ArrayList<>();

    boolean sale = true;
    @Nullable
    CompletableFuture<Maille> chantier;
    private int curseurLumiere;

    private final double centreX;
    private final double centreY;
    private final double centreZ;

    Tranche(int coinX, int coinY, int coinZ) {
      this.coinX = coinX;
      this.coinY = coinY;
      this.coinZ = coinZ;
      this.centreX = coinX + COTE / 2.0D;
      this.centreY = coinY + COTE / 2.0D;
      this.centreZ = coinZ + COTE / 2.0D;
    }

    double distanceCarree(Vec3 oeil) {
      double dx = centreX - oeil.x;
      double dy = centreY - oeil.y;
      double dz = centreZ - oeil.z;
      return dx * dx + dy * dy + dz * dz;
    }

    boolean enVue(Vec3 oeil, Vec3 regard, double portee) {
      double dx = centreX - oeil.x;
      double dy = centreY - oeil.y;
      double dz = centreZ - oeil.z;
      double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (distance - RAYON_TRANCHE > portee) {
        return false;
      }
      if (distance <= RAYON_TRANCHE) {
        return true;
      }
      double produit = dx * regard.x + dy * regard.y + dz * regard.z;
      return produit + RAYON_TRANCHE >= COSINUS_CHAMP * distance;
    }

    void adopter(Maille maille) {
      calques.clear();
      calques.putAll(maille.calques());
      blocs.clear();
      blocs.addAll(maille.blocs());
      curseurLumiere = 0;
    }

    int rafraichirEclairage(NavireEntity navire, int budget) {
      if (blocs.isEmpty() || budget <= 0) {
        return budget;
      }
      BlockPos.MutableBlockPos curseur = new BlockPos.MutableBlockPos();
      int restantes = budget;
      while (restantes > 0 && !blocs.isEmpty()) {
        if (curseurLumiere >= blocs.size()) {
          curseurLumiere = 0;
          break;
        }
        BlocCuit bloc = blocs.get(curseurLumiere++);
        curseur.set(bloc.x, bloc.y, bloc.z);
        bloc.lumiere = LevelRenderer.getLightColor(navire.level(), navire.positionMonde(curseur));
        restantes--;
      }
      return restantes;
    }
  }
}
