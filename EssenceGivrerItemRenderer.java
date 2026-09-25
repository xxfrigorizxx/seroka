package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.client.model.EssenceGivrerModel;
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

/** Rendu 3D de l'essence givrée — glace bleue (noyau) / glace (anneau extérieur). */
public class EssenceGivrerItemRenderer extends BlockEntityWithoutLevelRenderer {

  private static final ResourceLocation SPRITE_GLACE_BLEUE =
      ResourceLocation.withDefaultNamespace("block/blue_ice");
  private static final ResourceLocation SPRITE_GLACE =
      ResourceLocation.withDefaultNamespace("block/ice");

  private static final int COULEUR_INTERIEUR = 0xCC74B9F5;
  private static final int COULEUR_EXTERIEUR = 0xCCD0F0FF;

  private static final float Y_CENTRE_MODELE = 0.0F;
  private static final float FACTEUR_TAILLE = 3.0F;

  private static EssenceGivrerItemRenderer instance;

  private final EntityModelSet modelSet;
  private EssenceGivrerModel model;

  public EssenceGivrerItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
    super(dispatcher, modelSet);
    this.modelSet = modelSet;
    instance = this;
  }

  public static EssenceGivrerItemRenderer getInstance() {
    if (instance == null) {
      Minecraft client = Minecraft.getInstance();
      instance = new EssenceGivrerItemRenderer(
          client.getBlockEntityRenderDispatcher(),
          client.getEntityModels()
      );
    }
    return instance;
  }

  private EssenceGivrerModel obtenirModele() {
    if (model == null) {
      model = new EssenceGivrerModel(modelSet.bakeLayer(EssenceGivrerModel.LAYER));
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

    EssenceGivrerModel modele = obtenirModele();
    float ageInTicks = obtenirAgeAnimation();
    modele.animer(ageInTicks);

    Minecraft client = Minecraft.getInstance();
    TextureAtlasSprite spriteGlaceBleue = client
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(SPRITE_GLACE_BLEUE);
    TextureAtlasSprite spriteGlace = client
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(SPRITE_GLACE);

    float scroll = ageInTicks * 0.035F;
    int light = LightTexture.FULL_BRIGHT;
    RenderType renderType = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    VertexConsumer raw = ItemRenderer.getArmorFoilBuffer(
        buffer,
        renderType,
        stack.hasFoil()
    );

    VertexConsumer exterieurVertices = new FlowingSpriteVertexConsumer(
        raw,
        spriteGlace,
        scroll * 0.4F,
        scroll
    );
    modele.renderExterieur(poseStack, exterieurVertices, light, OverlayTexture.NO_OVERLAY, COULEUR_EXTERIEUR);

    VertexConsumer interieurVertices = new FlowingSpriteVertexConsumer(
        raw,
        spriteGlaceBleue,
        -scroll * 0.25F,
        scroll * 0.25F
    );
    modele.renderInterieur(poseStack, interieurVertices, light, OverlayTexture.NO_OVERLAY, COULEUR_INTERIEUR);

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

  private static void appliquerTransformationsAffichage(ItemDisplayContext context, PoseStack poseStack) {
    float echelle = (1.0F / 16.0F) * FACTEUR_TAILLE;
    float pivotY = Y_CENTRE_MODELE;

    switch (context) {
      case GUI -> echelle *= 2.9F;
      case GROUND -> {
        echelle *= 1.5F;
        pivotY = Y_CENTRE_MODELE;
      }
      case FIXED -> echelle *= 2.0F;
      case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> echelle *= 2.0F;
      case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> echelle *= 1.7F;
      default -> echelle *= 1.8F;
    }

    switch (context) {
      case GUI -> {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(30.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
      }
      case GROUND -> poseStack.translate(0.5F, 0.45F, 0.5F);
      case FIXED -> poseStack.translate(0.5F, 0.5F, 0.5F);
      case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> poseStack.translate(0.5F, 0.55F, 0.5F);
      case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> poseStack.translate(0.5F, 0.52F, 0.5F);
      default -> poseStack.translate(0.5F, 0.5F, 0.5F);
    }

    poseStack.scale(1.0F, -1.0F, -1.0F);
    poseStack.scale(echelle, echelle, echelle);
    poseStack.translate(0.0F, -pivotY, 0.0F);
  }
}
