package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.client.model.EssenceEauModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Rendu 3D de l'essence d'eau — eau vanilla teintée océan froid (noyau) / chaud (contour). */
public class EssenceEauItemRenderer extends BlockEntityWithoutLevelRenderer {

  /** Eau calme — noyau (océan froid). */
  private static final ResourceLocation SPRITE_EAU_CALME =
      ResourceLocation.withDefaultNamespace("block/water_still");
  /** Eau courante — anneau extérieur (océan chaud). */
  private static final ResourceLocation SPRITE_EAU_COURANTE =
      ResourceLocation.withDefaultNamespace("block/water_flow");

  /** Teinte eau océan froid (#2080C9) — centre. */
  private static final int COULEUR_CENTRE = 0xCC2080C9;
  /** Teinte eau océan chaud (#02B0E5) — contour. */
  private static final int COULEUR_CONTOUR = 0xCC02B0E5;

  /** Centre du modèle à l'origine (pivot Y pour inventaire et sol). */
  private static final float Y_CENTRE_MODELE = 0.0F;
  /** Facteur demandé : 3× plus grand que l'ancien rendu. */
  private static final float FACTEUR_TAILLE = 3.0F;

  private static EssenceEauItemRenderer instance;

  private final EntityModelSet modelSet;
  private EssenceEauModel model;

  public EssenceEauItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
    super(dispatcher, modelSet);
    this.modelSet = modelSet;
    instance = this;
  }

  public static EssenceEauItemRenderer getInstance() {
    if (instance == null) {
      Minecraft client = Minecraft.getInstance();
      instance = new EssenceEauItemRenderer(
          client.getBlockEntityRenderDispatcher(),
          client.getEntityModels()
      );
    }
    return instance;
  }

  private EssenceEauModel obtenirModele() {
    if (model == null) {
      model = new EssenceEauModel(modelSet.bakeLayer(EssenceEauModel.LAYER));
    }
    return model;
  }

  @Override
  public void renderByItem(
      ItemStack stack,
      ItemDisplayContext context,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      int packedOverlay
  ) {
    poseStack.pushPose();
    appliquerTransformationsAffichage(context, poseStack);

    EssenceEauModel modele = obtenirModele();
    float ageInTicks = obtenirAgeAnimation();
    modele.animer(ageInTicks);

    Minecraft client = Minecraft.getInstance();
    TextureAtlasSprite spriteCalme = client
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(SPRITE_EAU_CALME);
    TextureAtlasSprite spriteCourante = client
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(SPRITE_EAU_COURANTE);

    float scroll = ageInTicks * 0.035F;
    int light = LightTexture.FULL_BRIGHT;
    RenderType renderType = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    VertexConsumer raw = ItemRenderer.getArmorFoilBuffer(
        buffer,
        renderType,
        stack.hasFoil()
    );

    // Anneau extérieur : eau courante océan chaud.
    VertexConsumer contourVertices = new FlowingSpriteVertexConsumer(
        raw,
        spriteCourante,
        scroll * 0.4F,
        scroll
    );
    modele.renderContour(poseStack, contourVertices, light, OverlayTexture.NO_OVERLAY, COULEUR_CONTOUR);

    // Noyau : eau calme océan froid.
    VertexConsumer centreVertices = new FlowingSpriteVertexConsumer(
        raw,
        spriteCalme,
        -scroll * 0.25F,
        scroll * 0.25F
    );
    modele.renderCentre(poseStack, centreVertices, light, OverlayTexture.NO_OVERLAY, COULEUR_CENTRE);

    if (buffer instanceof MultiBufferSource.BufferSource immediate) {
      immediate.endBatch(renderType);
    }

    poseStack.popPose();
  }

  private static float obtenirAgeAnimation() {
    Minecraft client = Minecraft.getInstance();
    if (client.level != null) {
      float partialTick = client.getTimer().getGameTimeDeltaPartialTick(true);
      return client.level.getGameTime() + partialTick;
    }
    return (float) (System.currentTimeMillis() % 360000L) / 50.0F;
  }

  /**
   * Transforme le modèle (coords pixels) dans la case d'item 1×1.
   * Ordre : slot → flip → échelle → pivot pixels (en dernier).
   */
  private static void appliquerTransformationsAffichage(ItemDisplayContext context, PoseStack poseStack) {
    float echelle = (1.0F / 16.0F) * FACTEUR_TAILLE;
    float pivotY = Y_CENTRE_MODELE;

    switch (context) {
      case GUI -> {
        echelle *= 2.9F;
        break;
      }
      case GROUND -> {
        echelle *= 1.5F; // Ajustement mineur de la taille au sol
        pivotY = Y_CENTRE_MODELE; // Conserver le centre à 0.0F (évite projection vers le bas après flip)
        break;
      }
      case FIXED -> {
        echelle *= 2.0F;
        break;
      }
      case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
        echelle *= 2.0F;
        break;
      }
      case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
        echelle *= 1.7F;
        break;
      }
      default -> {
        echelle *= 1.8F;
        break;
      }
    }

    switch (context) {
      case GUI -> {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(30.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        break;
      }
      case GROUND -> {
        // Translation Y relevée pour stabiliser l'oscillation et éviter l'enfoncement au point bas.
        poseStack.translate(0.5F, 0.45F, 0.5F);
        break;
      }
      case FIXED -> {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        break;
      }
      case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
        poseStack.translate(0.5F, 0.55F, 0.5F);
        break;
      }
      case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
        poseStack.translate(0.5F, 0.52F, 0.5F);
        break;
      }
      default -> {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        break;
      }
    }

    poseStack.scale(1.0F, -1.0F, -1.0F);
    poseStack.scale(echelle, echelle, echelle);
    // Ramener le point pivot (centre ou bas) à l'origine du repère modèle.
    poseStack.translate(0.0F, -pivotY, 0.0F);
  }
}
