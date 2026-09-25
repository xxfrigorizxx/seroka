package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.navire.GreementNavire;
import com.seroka.navire.NavireEntity;
import com.seroka.navire.StructureNavire;
import com.seroka.navire.VoileNavire;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * Dessine les voiles d'une coque : une vraie toile, pas des cubes de laine.
 *
 * <p>Les blocs de laine ne servent qu'au charpentier, pour dire jusqu'où va la voile ; ils ne sont
 * pas rendus. À leur place on tend une nappe d'un seul tenant entre les deux vergues, creusée par le
 * vent d'autant plus que la voile est établie. Une voile à moitié dehors pend donc presque à plat,
 * une voile pleine se gonfle.
 *
 * <p>Replier ne retire aucun bloc : la nappe se raccourcit vers la vergue haute et la vergue du bas
 * remonte avec elle, comme une voile qu'on serre sur son rouleau.
 */
public final class GreementRenderer {

  /** Découpe de la nappe par bloc : c'est elle qui rend le creux lisse au lieu d'anguleux. */
  private static final int SEGMENTS_PAR_BLOC = 3;

  /** Creux maximal de la toile, en blocs, voile pleine et vent portant. */
  private static final float CREUX_MAX = 0.55F;

  /** Respiration de la toile : une voile vivante n'est jamais tout à fait immobile. */
  private static final float BATTEMENT = 0.18F;
  private static final float VITESSE_BATTEMENT = 0.08F;

  private GreementRenderer() {}

  public static void dessiner(NavireEntity navire, PoseStack poseStack, MultiBufferSource buffer,
      BlockRenderDispatcher blockRenderer, Vec3 oeil, double portee, float partialTick) {
    var voiles = navire.voiles();
    if (voiles.isEmpty()) {
      return;
    }
    StructureNavire structure = navire.structure();
    float temps = navire.tickCount + partialTick;
    for (VoileNavire voile : voiles.values()) {
      dessinerUneVoile(navire, structure, voile, poseStack, buffer, blockRenderer, oeil, portee,
          temps);
    }
  }

  private static void dessinerUneVoile(NavireEntity navire, StructureNavire structure,
      VoileNavire voile, PoseStack poseStack, MultiBufferSource buffer,
      BlockRenderDispatcher blockRenderer, Vec3 oeil, double portee, float temps) {
    if (voile.hauteurMax() <= 0 || horsDePortee(voile.ancre(), oeil, portee)) {
      return;
    }
    dessinerLaToile(navire, structure, voile, poseStack, buffer, blockRenderer, temps);
    dessinerLesVerguesBasses(navire, structure, voile, poseStack, buffer, blockRenderer);
  }

  // ---------------------------------------------------------------- la nappe

  /**
   * Tend la nappe, du dessous de la vergue haute jusqu'à son point bas du moment.
   *
   * <p>La toile est continue et non découpée en rangées de blocs : une voile à un tiers dehors mesure
   * un tiers de bloc, et il faut pouvoir la dessiner. Découper par blocs entiers laissait sinon un
   * vide sous la vergue, voire une voile entièrement invisible quand il n'y avait pas un bloc plein
   * de toile à sortir.
   */
  private static void dessinerLaToile(NavireEntity navire, StructureNavire structure,
      VoileNavire voile, PoseStack poseStack, MultiBufferSource buffer,
      BlockRenderDispatcher blockRenderer, float temps) {
    TextureAtlasSprite tissu = tissuDe(structure, voile, blockRenderer);
    if (tissu == null) {
      return;
    }
    VertexConsumer nappe = buffer.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
    boolean surX = voile.axe() == Direction.Axis.X;
    float creux = CREUX_MAX * voile.deploiement() * sensDuCreux(navire, surX);
    double sommet = voile.vergueHaut();

    for (int longAxe = voile.debut(); longAxe <= voile.fin(); longAxe++) {
      // Chaque colonne a sa propre hauteur et se replie au prorata : la découpe tient à tous les crans.
      double hauteurDehors = voile.deploiement() * voile.hauteurDeColonne(longAxe);
      if (hauteurDehors < 1.0E-3D) {
        continue;
      }
      double pied = sommet - hauteurDehors;
      // Le motif du tissu se répète par bloc en partant de la têtière : on découpe donc là.
      int bandes = Mth.ceil(hauteurDehors);
      for (int bande = 0; bande < bandes; bande++) {
        double haut = sommet - bande;
        double bas = Math.max(pied, haut - 1.0D);
        if (haut - bas < 1.0E-4D) {
          continue;
        }
        // Serrer une voile la roule par le bas : ce qui reste dehors est le haut de la toile, donc
        // la bande n° n montre toujours la n-ième case sous la vergue haute, trous compris.
        BlockPos place = voile.offset(longAxe, voile.vergueHaut() - 1 - bande);
        if (!GreementNavire.estToile(structure.bloc(place))) {
          // Cette case a été emportée : la voile est trouée pour de bon, pas seulement moins vive.
          continue;
        }
        int lumiere = LevelRenderer.getLightColor(navire.level(), navire.positionMonde(place));
        dessinerUneBande(nappe, poseStack, voile, longAxe, haut, bas, hauteurDehors, creux,
            tissu, lumiere, temps, surX);
      }
    }
  }

  /**
   * Une bande de toile d'un bloc de haut au plus, découpée en carreaux pour suivre le creux.
   *
   * <p>L'atlas ne sait pas répéter un motif : chaque bande remet donc l'image entière du tissu, et la
   * bande du bas la coupe à la hauteur qu'il reste de toile dehors.
   */
  private static void dessinerUneBande(VertexConsumer nappe, PoseStack poseStack, VoileNavire voile,
      int longAxe, double haut, double bas, double hauteurDehors, float creux,
      TextureAtlasSprite tissu, int lumiere, float temps, boolean surX) {
    double plan = (surX ? voile.offset(longAxe, 0).getZ() : voile.offset(longAxe, 0).getX()) + 0.5D;
    float hauteurBande = (float) (haut - bas);
    float pas = 1.0F / SEGMENTS_PAR_BLOC;

    for (int i = 0; i < SEGMENTS_PAR_BLOC; i++) {
      for (int j = 0; j < SEGMENTS_PAR_BLOC; j++) {
        float s0 = i * pas;
        float s1 = s0 + pas;
        // Mesuré depuis le haut de la bande : c'est aussi la coordonnée de texture.
        float t0 = j * pas * hauteurBande;
        float t1 = (j + 1) * pas * hauteurBande;

        double[] coins = new double[] {
            longAxe + s0, haut - t0,
            longAxe + s1, haut - t0,
            longAxe + s1, haut - t1,
            longAxe + s0, haut - t1,
        };
        float[] u = new float[] {
            tissu.getU(s0), tissu.getU(s1), tissu.getU(s1), tissu.getU(s0)
        };
        float[] v = new float[] {
            tissu.getV(t0), tissu.getV(t0), tissu.getV(t1), tissu.getV(t1)
        };

        // Deux passes : une toile n'a pas d'épaisseur, il faut la voir des deux bords.
        poserCarreau(nappe, poseStack, voile, coins, u, v, plan, creux, hauteurDehors, lumiere,
            temps, surX, false);
        poserCarreau(nappe, poseStack, voile, coins, u, v, plan, creux, hauteurDehors, lumiere,
            temps, surX, true);
      }
    }
  }

  private static void poserCarreau(VertexConsumer nappe, PoseStack poseStack, VoileNavire voile,
      double[] coins, float[] u, float[] v, double plan, float creux, double hauteurDehors,
      int lumiere, float temps, boolean surX, boolean envers) {
    PoseStack.Pose pose = poseStack.last();
    float normale = (creux >= 0.0F ? 1.0F : -1.0F) * (envers ? -1.0F : 1.0F);
    for (int coin = 0; coin < 4; coin++) {
      int rang = envers ? 3 - coin : coin;
      double longAxe = coins[rang * 2];
      double y = coins[rang * 2 + 1];
      double ecart = plan + creuxAu(voile, longAxe, y, creux, hauteurDehors, temps);
      double x = surX ? longAxe : ecart;
      double z = surX ? ecart : longAxe;
      nappe.addVertex(pose, (float) x, (float) y, (float) z)
          .setColor(0xFFFFFFFF)
          .setUv(u[rang], v[rang])
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(lumiere)
          .setNormal(pose, surX ? 0.0F : normale, 0.0F, surX ? normale : 0.0F);
    }
  }

  /**
   * Creux de la toile en ce point : nul aux ralingues, maximal au milieu.
   *
   * <p>La voile est tenue par ses deux vergues et ses bords : elle se gonfle donc comme un coussin,
   * et non comme une plaque poussée à plat. Un léger battement dans le temps l'empêche de paraître
   * figée quand le navire est au mouillage.
   */
  private static float creuxAu(VoileNavire voile, double longAxe, double y, float creux,
      double hauteurDehors, float temps) {
    if (Math.abs(creux) < 1.0E-4F) {
      return 0.0F;
    }
    float u = (float) ((longAxe - voile.debut()) / Math.max(1.0D, voile.largeur()));
    float v = (float) ((voile.vergueHaut() - y) / Math.max(1.0E-3D, hauteurDehors));
    float coussin = Mth.sin(u * Mth.PI) * Mth.sin(Mth.clamp(v, 0.0F, 1.0F) * Mth.PI);
    float souffle = 1.0F + BATTEMENT * Mth.sin(temps * VITESSE_BATTEMENT + u * 3.0F);
    return creux * coussin * souffle;
  }

  /** De quel bord la toile se creuse : le vent la pousse vers l'avant du navire. */
  private static float sensDuCreux(NavireEntity navire, boolean surX) {
    Direction proue = navire.proueLocale();
    int composante = surX ? proue.getStepZ() : proue.getStepX();
    return composante == 0 ? 1.0F : composante;
  }

  /** Texture de la laine que le charpentier a tendue : c'est elle qui habille la nappe. */
  @Nullable
  private static TextureAtlasSprite tissuDe(StructureNavire structure, VoileNavire voile,
      BlockRenderDispatcher blockRenderer) {
    for (int longAxe = voile.debut(); longAxe <= voile.fin(); longAxe++) {
      BlockState laine = structure.bloc(voile.offset(longAxe, voile.vergueHaut() - 1));
      if (GreementNavire.estToile(laine)) {
        return blockRenderer.getBlockModel(laine).getParticleIcon(ModelData.EMPTY);
      }
    }
    return null;
  }

  // --------------------------------------------------------------- la vergue

  private static void dessinerLesVerguesBasses(NavireEntity navire, StructureNavire structure,
      VoileNavire voile, PoseStack poseStack, MultiBufferSource buffer,
      BlockRenderDispatcher blockRenderer) {
    for (int longAxe = voile.debut(); longAxe <= voile.fin(); longAxe++) {
      if (!voile.estChaussee(longAxe)) {
        continue;
      }
      // Chaque colonne remonte de sa propre course : une vergue oblique reste oblique en se serrant.
      double montee = (1.0D - voile.deploiement()) * voile.hauteurDeColonne(longAxe);
      BlockPos place = voile.offset(longAxe, voile.basDeColonne(longAxe) - 1);
      BlockState etat = structure.bloc(place);
      if (etat.isAir()) {
        continue;
      }
      int lumiere = LevelRenderer.getLightColor(navire.level(), navire.positionMonde(place));
      poseStack.pushPose();
      poseStack.translate(place.getX(), place.getY() + montee, place.getZ());
      blockRenderer.renderSingleBlock(etat, poseStack, buffer, lumiere, OverlayTexture.NO_OVERLAY,
          ModelData.EMPTY, null);
      poseStack.popPose();
    }
  }

  private static boolean horsDePortee(BlockPos offset, Vec3 oeil, double portee) {
    double dx = offset.getX() + 0.5D - oeil.x;
    double dy = offset.getY() + 0.5D - oeil.y;
    double dz = offset.getZ() + 0.5D - oeil.z;
    return dx * dx + dy * dy + dz * dz > portee * portee;
  }
}
