package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seroka.item.MarteauReparationItem;
import com.seroka.navire.NavireEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Dessine la coque d'un navire assemblé autour de son pivot.
 *
 * <p>La couture se fait une fois, par tranches ; chaque image ne fait que recopier les faces déjà
 * prêtes dans le tampon des entités. Ni la barre, ni le pont, ni les collisions ne passent par ici.
 */
public class NavireRenderer extends EntityRenderer<NavireEntity> {

  private static final double DISTANCE_MOBILIER = 40.0D;

  private static final int MOBILIER_MAX = 48;

  private static final double COSINUS_CHAMP = -0.35D;

  private static final double DISTANCE_TOUJOURS_VISIBLE = 6.0D;

  /** Portée d'affichage des planches manquantes : de quoi repérer la brèche du pont. */
  private static final double PORTEE_FANTOMES = 32.0D;

  /** Fantômes dessinés au maximum : une grande avarie ne doit pas coûter l'image. */
  private static final int FANTOMES_MAX = 512;

  private static final float OPACITE_FANTOME = 0.4F;

  private final BlockRenderDispatcher blockRenderer;
  private final BlockEntityRenderDispatcher mobilierRenderer;
  private final Map<NavireEntity, CoqueMaillee> coques = new WeakHashMap<>();
  private final MobilierCoque mobilier = new MobilierCoque();

  public NavireRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.shadowRadius = 0.0F;
    this.blockRenderer = context.getBlockRenderDispatcher();
    this.mobilierRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher();
  }

  @Override
  public void render(NavireEntity navire, float yaw, float partialTick, PoseStack poseStack,
      MultiBufferSource buffer, int packedLight) {
    if (navire.structure().estVide()) {
      return;
    }

    float capRadians = (float) Math.toRadians(yaw);
    Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
    Vec3 oeil = camera.getPosition().subtract(navire.position()).yRot(capRadians);
    Vector3f vue = camera.getLookVector();
    Vec3 regard = new Vec3(vue.x(), vue.y(), vue.z()).yRot(capRadians);
    double portee = porteeDeRendu();

    CoqueMaillee coque = coques.computeIfAbsent(navire, ignore -> new CoqueMaillee());
    coque.preparer(navire, this.blockRenderer, Minecraft.getInstance().getBlockColors(), oeil);

    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
    incliner(navire, poseStack, partialTick);

    coque.dessiner(poseStack, buffer, oeil, regard, portee);
    CanonAssemblyRenderer.dessinerNavire(navire, poseStack, buffer, blockRenderer, oeil, portee,
        partialTick);
    GreementRenderer.dessiner(navire, poseStack, buffer, blockRenderer, oeil, portee, partialTick);
    dessinerMobilier(mobilier.pour(navire), partialTick, poseStack, buffer, oeil, regard);
    if (marteauEnMain()) {
      dessinerBreches(navire, poseStack, buffer, oeil);
    }

    poseStack.popPose();
    super.render(navire, yaw, partialTick, poseStack, buffer, packedLight);
  }

  private void dessinerMobilier(MobilierCoque.Cargaison cargaison, float partialTick,
      PoseStack poseStack, MultiBufferSource buffer, Vec3 oeil, Vec3 regard) {
    int dessines = 0;
    for (MobilierCoque.Meuble meuble : cargaison.meubles) {
      if (dessines >= MOBILIER_MAX) {
        return;
      }
      BlockPos offset = meuble.offset();
      double dx = offset.getX() + 0.5D - oeil.x;
      double dy = offset.getY() + 0.5D - oeil.y;
      double dz = offset.getZ() + 0.5D - oeil.z;
      double distanceCarree = dx * dx + dy * dy + dz * dz;
      if (distanceCarree > DISTANCE_MOBILIER * DISTANCE_MOBILIER
          || horsDuChamp(dx, dy, dz, distanceCarree, regard)) {
        continue;
      }
      poseStack.pushPose();
      poseStack.translate(offset.getX(), offset.getY(), offset.getZ());
      this.mobilierRenderer.render(meuble.entite(), partialTick, poseStack, buffer);
      poseStack.popPose();
      dessines++;
    }
  }

  /**
   * Montre en fantômes les planches qu'un choc a emportées.
   *
   * <p>Marteau en main, le charpentier voit ainsi le plan de la coque d'origine se dessiner dans le
   * trou : il sait où frapper, et avec quoi, sans tourner autour de l'épave à chercher la brèche.
   */
  private void dessinerBreches(NavireEntity navire, PoseStack poseStack, MultiBufferSource buffer,
      Vec3 oeil) {
    MultiBufferSource fantomes = type -> new FantomeVertexConsumer(buffer.getBuffer(type), OPACITE_FANTOME);
    dessinerFantomes(navire.breches().entrySet(), poseStack, fantomes, oeil, 0);
  }

  private int dessinerFantomes(Iterable<Map.Entry<BlockPos, BlockState>> blocs, PoseStack poseStack,
      MultiBufferSource fantomes, Vec3 oeil, int dejaDessines) {
    int dessinees = dejaDessines;
    for (Map.Entry<BlockPos, BlockState> entree : blocs) {
      if (dessinees >= FANTOMES_MAX) {
        return dessinees;
      }
      dessinees = dessinerUnFantome(entree.getKey(), entree.getValue(), poseStack, fantomes, oeil,
          dessinees);
    }
    return dessinees;
  }

  private int dessinerUnFantome(BlockPos offset, BlockState matiere, PoseStack poseStack,
      MultiBufferSource fantomes, Vec3 oeil, int dejaDessines) {
    double dx = offset.getX() + 0.5D - oeil.x;
    double dy = offset.getY() + 0.5D - oeil.y;
    double dz = offset.getZ() + 0.5D - oeil.z;
    if (dx * dx + dy * dy + dz * dz > PORTEE_FANTOMES * PORTEE_FANTOMES) {
      return dejaDessines;
    }
    poseStack.pushPose();
    poseStack.translate(offset.getX(), offset.getY(), offset.getZ());
    this.blockRenderer.renderSingleBlock(
        matiere,
        poseStack,
        fantomes,
        LightTexture.FULL_BRIGHT,
        OverlayTexture.NO_OVERLAY,
        ModelData.EMPTY,
        RenderType.translucent());
    poseStack.popPose();
    return dejaDessines + 1;
  }

  private static boolean marteauEnMain() {
    Player joueur = Minecraft.getInstance().player;
    return joueur != null
        && (joueur.getMainHandItem().getItem() instanceof MarteauReparationItem
            || joueur.getOffhandItem().getItem() instanceof MarteauReparationItem);
  }

  private static boolean horsDuChamp(double dx, double dy, double dz, double distanceCarree,
      Vec3 regard) {
    if (distanceCarree < DISTANCE_TOUJOURS_VISIBLE * DISTANCE_TOUJOURS_VISIBLE) {
      return false;
    }
    double produit = dx * regard.x + dy * regard.y + dz * regard.z;
    return produit < COSINUS_CHAMP * Math.sqrt(distanceCarree);
  }

  private static double porteeDeRendu() {
    int chunks = Minecraft.getInstance().options.getEffectiveRenderDistance();
    return Math.min(chunks * 16.0D, 128.0D);
  }

  private static void incliner(NavireEntity navire, PoseStack poseStack, float partialTick) {
    Direction proue = navire.proueLocale();
    float roulis = navire.roulis(partialTick);
    if (Math.abs(roulis) > 0.01F) {
      // Roulis du virage : autour de l'axe long de la coque, donc celui que suit sa proue.
      if (proue.getAxis() == Direction.Axis.Z) {
        poseStack.mulPose(Axis.ZP.rotationDegrees(roulis * proue.getStepZ()));
      } else {
        poseStack.mulPose(Axis.XP.rotationDegrees(-roulis * proue.getStepX()));
      }
    }
    float gite = navire.gite(partialTick);
    if (gite <= 0.0F) {
      return;
    }
    if (proue.getAxis() == Direction.Axis.Z) {
      poseStack.mulPose(Axis.XP.rotationDegrees(gite * proue.getStepZ()));
    } else {
      poseStack.mulPose(Axis.ZP.rotationDegrees(-gite * proue.getStepX()));
    }
  }

  @Override
  public ResourceLocation getTextureLocation(NavireEntity navire) {
    return InventoryMenu.BLOCK_ATLAS;
  }
}
