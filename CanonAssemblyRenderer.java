package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.ModMain;
import com.seroka.ModRegistry;
import com.seroka.block.CanonBlock;
import com.seroka.block.CanonBlockEntity;
import com.seroka.block.CanonPartie;
import com.seroka.client.CanonOrientationClient;
import com.seroka.client.CanonReculClient;
import com.seroka.client.CanonViseeClientHandler;
import com.seroka.navire.CanonLogique;
import com.seroka.navire.NavireEntity;
import com.seroka.navire.StructureNavire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * Affût (2 blocs) + tube (2 blocs). Après orientation du bloc :
 * <ul>
 *   <li>latéral ({@code traverse}) : tout le canon tourne autour du centre de la culasse ;</li>
 *   <li>hauteur ({@code pitch}) : seul le tube pivote sur l'affût.</li>
 * </ul>
 */
public final class CanonAssemblyRenderer {

  private static final ModelResourceLocation AFFUT_CULASSE =
      ModelResourceLocation.standalone(ModMain.id("canon/affut_culasse"));
  private static final ModelResourceLocation AFFUT_BOUCHE =
      ModelResourceLocation.standalone(ModMain.id("canon/affut_bouche"));
  private static final ModelResourceLocation TUBE_CULASSE =
      ModelResourceLocation.standalone(ModMain.id("canon/tube_culasse"));
  private static final ModelResourceLocation TUBE_BOUCHE =
      ModelResourceLocation.standalone(ModMain.id("canon/tube_bouche"));

  private static final BlockState ETAT_RENDU = ModRegistry.CANON.get().defaultBlockState()
      .setValue(CanonBlock.PARTIE, CanonPartie.CULASSE);

  /** Charnière du tube sur la culasse (repère modèle : bouche vers +Z). */
  private static final double PIVOT_Y = 0.625D;
  private static final double DECALAGE_BOUCHE = 1.0D;

  private CanonAssemblyRenderer() {}

  public static void dessinerNavire(NavireEntity navire, PoseStack poseStack,
      MultiBufferSource buffer, BlockRenderDispatcher blockRenderer, Vec3 oeil, double portee,
      float partialTick) {
    StructureNavire structure = navire.structure();
    structure.parcourir((x, y, z, etat) -> {
      if (!estCulasse(etat)) {
        return;
      }
      BlockPos culasse = new BlockPos(x, y, z);
      double dx = culasse.getX() + 0.5D - oeil.x;
      double dy = culasse.getY() + 0.5D - oeil.y;
      double dz = culasse.getZ() + 0.5D - oeil.z;
      if (dx * dx + dy * dy + dz * dz > portee * portee) {
        return;
      }
      Direction visee = etat.getValue(CanonBlock.FACING);
      float yaw = CanonOrientationClient.yawNavire(navire, culasse, visee);
      float pitch = CanonOrientationClient.pitchNavire(navire, culasse);
      if (CanonViseeClientHandler.viseCeCanon(navire.getId(), culasse)) {
        yaw = CanonViseeClientHandler.yawVisée(visee);
        pitch = CanonViseeClientHandler.pitchVisée();
      }
      int lumiere = LevelRenderer.getLightColor(navire.level(), navire.positionMonde(culasse));
      int lumiereBouche = LevelRenderer.getLightColor(navire.level(),
          navire.positionMonde(culasse.relative(visee)));
      float recul = CanonReculClient.recul(navire.getId(), culasse, partialTick);
      poseStack.pushPose();
      poseStack.translate(culasse.getX(), culasse.getY(), culasse.getZ());
      dessinerAssemblage(poseStack, buffer, blockRenderer, navire, visee, yaw, pitch, recul,
          lumiere, lumiereBouche);
      poseStack.popPose();
    });
  }

  public static void dessinerMonde(CanonBlockEntity entite, PoseStack poseStack,
      MultiBufferSource buffer, BlockRenderDispatcher blockRenderer, int packedLight,
      float partialTick) {
    BlockState etat = entite.getBlockState();
    if (!estCulasse(etat) || entite.getLevel() == null) {
      return;
    }
    Direction visee = etat.getValue(CanonBlock.FACING);
    BlockPos culasse = entite.getBlockPos();
    BlockPos bouche = culasse.relative(visee);
    int lumiereBouche = LevelRenderer.getLightColor(entite.getLevel(), bouche);
    float yaw = Float.isNaN(entite.viseeYaw())
        ? CanonLogique.yawPourDirection(visee) : entite.viseeYaw();
    float pitch = Float.isNaN(entite.viseePitch()) ? 0.0F : entite.viseePitch();
    if (CanonViseeClientHandler.viseCanonAuSol(culasse)) {
      yaw = CanonViseeClientHandler.yawVisée(visee);
      pitch = CanonViseeClientHandler.pitchVisée();
    }
    float recul = CanonReculClient.recul(-1, culasse, partialTick);
    dessinerAssemblage(poseStack, buffer, blockRenderer, null, visee, yaw, pitch, recul,
        packedLight, lumiereBouche);
  }

  private static void dessinerAssemblage(PoseStack poseStack, MultiBufferSource buffer,
      BlockRenderDispatcher blockRenderer, @Nullable NavireEntity navire, Direction visee,
      float yawDeg, float pitchDeg, float recul, int lumiereCulasse, int lumiereBouche) {
    var gestionnaire = Minecraft.getInstance().getModelManager();
    BakedModel affutC = gestionnaire.getModel(AFFUT_CULASSE);
    BakedModel affutB = gestionnaire.getModel(AFFUT_BOUCHE);
    BakedModel tubeC = gestionnaire.getModel(TUBE_CULASSE);
    BakedModel tubeB = gestionnaire.getModel(TUBE_BOUCHE);
    BakedModel manquant = gestionnaire.getMissingModel();

    if (affutC == manquant && tubeC == manquant) {
      poseStack.pushPose();
      orienterSelonFacing(poseStack, visee);
      dessinerSecours(poseStack, buffer, blockRenderer, lumiereCulasse, lumiereBouche);
      poseStack.popPose();
      return;
    }

    float yawBase = navire != null
        ? CanonLogique.yawMondeNavire(navire, visee)
        : CanonLogique.yawPourDirection(visee);
    float traverse = Mth.wrapDegrees(yawDeg - yawBase);
    float pitchRad = CanonLogique.inclinaisonDuTube(pitchDeg);

    poseStack.pushPose();
    orienterSelonFacing(poseStack, visee);

    // Débattement latéral : affût + tube ensemble. Le lacet de Minecraft tourne à l'envers du
    // repère de rendu, comme pour l'orientation du bloc juste au-dessus : d'où le signe.
    poseStack.translate(0.5D, 0.0D, 0.5D);
    poseStack.mulPose(Axis.YP.rotationDegrees(-traverse));
    poseStack.translate(-0.5D, 0.0D, -0.5D);

    // Recul : tout l'attelage part vers l'arrière, donc à l'opposé de la bouche.
    poseStack.translate(0.0D, 0.0D, -recul);

    if (affutC != manquant) {
      dessinerModele(affutC, poseStack, buffer, lumiereCulasse);
    }
    poseStack.pushPose();
    translaterVersBouche(poseStack);
    if (affutB != manquant) {
      dessinerModele(affutB, poseStack, buffer, lumiereBouche);
    }
    poseStack.popPose();

    // Élévation : tube seul, pivot côté culasse (z = 0 local, arrière du canon).
    poseStack.pushPose();
    poseStack.translate(0.5D, PIVOT_Y, 0.0D);
    poseStack.mulPose(Axis.XP.rotation(pitchRad));
    poseStack.translate(-0.5D, -PIVOT_Y, 0.0D);
    if (tubeC != manquant) {
      dessinerModele(tubeC, poseStack, buffer, lumiereCulasse);
    }
    poseStack.pushPose();
    translaterVersBouche(poseStack);
    if (tubeB != manquant) {
      dessinerModele(tubeB, poseStack, buffer, lumiereBouche);
    }
    poseStack.popPose();
    poseStack.popPose();

    poseStack.popPose();
  }

  /** Aligne le modèle (bouche vers +Z) sur le {@link CanonBlock#FACING} du bloc. */
  private static void orienterSelonFacing(PoseStack poseStack, Direction visee) {
    poseStack.translate(0.5D, 0.0D, 0.5D);
    poseStack.mulPose(Axis.YP.rotationDegrees(-visee.toYRot()));
    poseStack.translate(-0.5D, 0.0D, -0.5D);
  }

  /** Bloc bouche = culasse + {@code FACING} ; après orientation, c'est +Z local. */
  private static void translaterVersBouche(PoseStack poseStack) {
    poseStack.translate(0.0D, 0.0D, DECALAGE_BOUCHE);
  }

  private static void dessinerModele(BakedModel modele, PoseStack poseStack,
      MultiBufferSource buffer, int packedLight) {
    RandomSource des = RandomSource.create(42L);
    for (RenderType calque : modele.getRenderTypes(ETAT_RENDU, des, ModelData.EMPTY)) {
      VertexConsumer consommateur = buffer.getBuffer(calque);
      for (BakedQuad quad : modele.getQuads(ETAT_RENDU, null, des)) {
        consommateur.putBulkData(poseStack.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F,
            packedLight, OverlayTexture.NO_OVERLAY);
      }
    }
  }

  private static void dessinerSecours(PoseStack poseStack, MultiBufferSource buffer,
      BlockRenderDispatcher blockRenderer, int lumiereCulasse, int lumiereBouche) {
    BlockState culasse = ETAT_RENDU;
    BlockState bouche = culasse.setValue(CanonBlock.PARTIE, CanonPartie.BOUCHE);
    blockRenderer.renderSingleBlock(culasse, poseStack, buffer, lumiereCulasse, OverlayTexture.NO_OVERLAY,
        ModelData.EMPTY, null);
    poseStack.pushPose();
    translaterVersBouche(poseStack);
    blockRenderer.renderSingleBlock(bouche, poseStack, buffer, lumiereBouche, OverlayTexture.NO_OVERLAY,
        ModelData.EMPTY, null);
    poseStack.popPose();
  }

  private static boolean estCulasse(BlockState etat) {
    return etat.getBlock() instanceof CanonBlock
        && etat.getValue(CanonBlock.PARTIE) == CanonPartie.CULASSE;
  }
}
