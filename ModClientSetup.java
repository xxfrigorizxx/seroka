package com.seroka.client;

import com.seroka.ModRegistry;
import com.seroka.catalog.CatalogMobRegistry;
import com.seroka.client.renderer.CatalogMobRenderer;
import com.seroka.client.renderer.PlayerIllusionRenderer;
import com.seroka.client.renderer.ThrownSwordRenderer;
import com.seroka.client.renderer.WaterBallRenderer;
import com.seroka.client.renderer.BloodBallRenderer;
import com.seroka.client.renderer.LavaBallRenderer;
import com.seroka.client.renderer.BloodSpikeRenderer;
import com.seroka.client.model.EssenceEauModel;
import com.seroka.client.model.EssenceLaveModel;
import com.seroka.client.model.EssenceGivrerModel;
import com.seroka.client.model.IceSpikeModel;
import com.seroka.client.model.MegaPicGlaceModel;
import com.seroka.client.model.WaterShieldModel;
import com.seroka.client.model.WaterCocoonModel;
import com.seroka.client.model.WaterSlideBootsModel;
import com.seroka.client.model.EarthSlideBootsModel;
import com.seroka.client.model.WaterPillarModel;
import com.seroka.client.model.PillardRockModel;
import com.seroka.client.model.GolemArmorModel;
import com.seroka.client.renderer.EssenceEauItemRenderer;
import com.seroka.client.renderer.EssenceLaveItemRenderer;
import com.seroka.client.renderer.EssenceGivrerItemRenderer;
import com.seroka.client.renderer.IceCrushRenderer;
import com.seroka.client.renderer.IcePrisonRenderer;
import com.seroka.client.renderer.IceSpikeRenderer;
import com.seroka.client.renderer.MegaPicGlaceRenderer;
import com.seroka.client.renderer.MegaPicTerreRenderer;
import com.seroka.client.renderer.WaterCocoonRenderer;
import com.seroka.client.renderer.WaterPillarRenderer;
import com.seroka.client.renderer.WaterShieldRenderer;
import com.seroka.client.renderer.WaterSlideRenderer;
import com.seroka.client.renderer.WaterSlashRenderer;
import com.seroka.client.renderer.WaterSiphonRenderer;
import com.seroka.client.renderer.BloodSiphonRenderer;
import com.seroka.client.renderer.BloodTornadoRenderer;
import com.seroka.client.renderer.BloodSlashRenderer;
import com.seroka.client.renderer.EarthSlideRenderer;
import com.seroka.client.renderer.EarthBlockRenderer;
import com.seroka.client.renderer.EarthCrushRenderer;
import com.seroka.client.renderer.EarthPillarRenderer;
import com.seroka.client.renderer.EarthDomeShardRenderer;
import com.seroka.client.renderer.MeteoreTelluriqueRenderer;
import com.seroka.client.renderer.RampeTelluriqueRenderer;
import com.seroka.client.renderer.DisqueTelluriqueRenderer;
import com.seroka.client.renderer.LameSismiqueRenderer;
import com.seroka.client.renderer.WaterWaveRenderer;
import com.seroka.client.renderer.IceWaveRenderer;
import com.seroka.client.renderer.layer.ArmureTerreLayer;
import com.seroka.client.renderer.layer.GanteletsTerreLayer;
import com.seroka.client.renderer.layer.GolemArmorLayer;
import com.seroka.client.renderer.layer.ArmureTerreLivingLayer;
import com.seroka.client.renderer.layer.ArmureEauLayer;
import com.seroka.client.renderer.layer.ArmureEauLivingLayer;
import com.seroka.client.renderer.layer.ArmureSangLayer;
import com.seroka.client.renderer.layer.ArmureSangLivingLayer;
import com.seroka.client.EssenceGivrerClientExtensions;
import com.seroka.client.screen.AnePackScreen;
import com.seroka.client.screen.ChevalSaddlebagScreen;
import com.seroka.client.screen.LamaPackScreen;
import com.seroka.client.screen.MulePackScreen;
import com.seroka.client.screen.NavireScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import com.seroka.ModMain;
import com.seroka.client.renderer.CanonAssemblyRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class ModClientSetup {

  private ModClientSetup() {}

  public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
    event.registerLayerDefinition(EssenceEauModel.LAYER, EssenceEauModel::createBodyLayer);
    event.registerLayerDefinition(EssenceLaveModel.LAYER, EssenceLaveModel::createBodyLayer);
    event.registerLayerDefinition(EssenceGivrerModel.LAYER, EssenceGivrerModel::createBodyLayer);
    event.registerLayerDefinition(WaterShieldModel.LAYER, WaterShieldModel::createBodyLayer);
    event.registerLayerDefinition(IceSpikeModel.LAYER, IceSpikeModel::createBodyLayer);
    event.registerLayerDefinition(MegaPicGlaceModel.LAYER, MegaPicGlaceModel::createBodyLayer);
    event.registerLayerDefinition(WaterCocoonModel.LAYER, WaterCocoonModel::createBodyLayer);
    event.registerLayerDefinition(WaterSlideBootsModel.LAYER, WaterSlideBootsModel::createBodyLayer);
    event.registerLayerDefinition(EarthSlideBootsModel.LAYER, EarthSlideBootsModel::createBodyLayer);
    event.registerLayerDefinition(WaterPillarModel.LAYER, WaterPillarModel::createBodyLayer);
    event.registerLayerDefinition(PillardRockModel.LAYER_LOCATION, PillardRockModel::createBodyLayer);
    event.registerLayerDefinition(GolemArmorModel.LAYER, GolemArmorModel::createBodyLayer);
  }

  /** Modèles 3D du canon (hors blockstates) : obligatoire pour qu'ils soient visibles en jeu. */
  public static void registerCanonModels(ModelEvent.RegisterAdditional event) {
    event.register(ModelResourceLocation.standalone(ModMain.id("canon/affut_culasse")));
    event.register(ModelResourceLocation.standalone(ModMain.id("canon/affut_bouche")));
    event.register(ModelResourceLocation.standalone(ModMain.id("canon/tube_culasse")));
    event.register(ModelResourceLocation.standalone(ModMain.id("canon/tube_bouche")));
  }

  public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(ModRegistry.CANON_BE.get(),
        com.seroka.client.renderer.CanonBlockEntityRenderer::new);
  }

  public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(ModRegistry.BOULET_CANON.get(),
        com.seroka.client.renderer.BouletCanonRenderer::new);
    event.registerEntityRenderer(ModRegistry.THROWN_SWORD.get(), ThrownSwordRenderer::new);
    event.registerEntityRenderer(ModRegistry.WATER_BALL.get(), WaterBallRenderer::new);
    event.registerEntityRenderer(ModRegistry.BLOOD_BALL.get(), BloodBallRenderer::new);
    event.registerEntityRenderer(ModRegistry.LAVA_BALL.get(), LavaBallRenderer::new);
    event.registerEntityRenderer(ModRegistry.BLOOD_SPIKE.get(), BloodSpikeRenderer::new);
    event.registerEntityRenderer(ModRegistry.WATER_SHIELD.get(), WaterShieldRenderer::new);
    event.registerEntityRenderer(ModRegistry.ICE_SPIKE.get(), IceSpikeRenderer::new);
    event.registerEntityRenderer(ModRegistry.MEGA_PIC_GLACE.get(), MegaPicGlaceRenderer::new);
    event.registerEntityRenderer(ModRegistry.MEGA_PIC_TERRE.get(), MegaPicTerreRenderer::new);
    event.registerEntityRenderer(ModRegistry.WATER_COCOON.get(), WaterCocoonRenderer::new);
    event.registerEntityRenderer(ModRegistry.WATER_SLIDE.get(), WaterSlideRenderer::new);
    event.registerEntityRenderer(ModRegistry.EARTH_SLIDE.get(), EarthSlideRenderer::new);
    event.registerEntityRenderer(ModRegistry.WATER_SLASH.get(), WaterSlashRenderer::new);
    event.registerEntityRenderer(ModRegistry.WATER_SIPHON.get(), WaterSiphonRenderer::new);
    event.registerEntityRenderer(ModRegistry.BLOOD_SIPHON.get(), BloodSiphonRenderer::new);
    event.registerEntityRenderer(ModRegistry.BLOOD_TORNADO.get(), BloodTornadoRenderer::new);
    event.registerEntityRenderer(ModRegistry.BLOOD_SLASH.get(), BloodSlashRenderer::new);
    event.registerEntityRenderer(ModRegistry.WATER_WAVE.get(), WaterWaveRenderer::new);
    event.registerEntityRenderer(ModRegistry.ICE_WAVE.get(), IceWaveRenderer::new);
    event.registerEntityRenderer(ModRegistry.WATER_PILLAR.get(), WaterPillarRenderer::new);
    event.registerEntityRenderer(ModRegistry.ICE_PRISON.get(), IcePrisonRenderer::new);
    event.registerEntityRenderer(ModRegistry.ICE_CRUSH.get(), IceCrushRenderer::new);
    event.registerEntityRenderer(ModRegistry.NAVIRE.get(), com.seroka.client.renderer.NavireRenderer::new);
    event.registerEntityRenderer(ModRegistry.EARTH_BLOCK.get(), EarthBlockRenderer::new);
    event.registerEntityRenderer(ModRegistry.EARTH_CRUSH.get(), EarthCrushRenderer::new);
    event.registerEntityRenderer(ModRegistry.EARTH_PILLAR.get(), EarthPillarRenderer::new);
    event.registerEntityRenderer(ModRegistry.EARTH_DOME_SHARD.get(), EarthDomeShardRenderer::new);
    event.registerEntityRenderer(ModRegistry.METEORE_TELLURIQUE.get(), MeteoreTelluriqueRenderer::new);
    event.registerEntityRenderer(ModRegistry.RAMPE_TELLURIQUE.get(), RampeTelluriqueRenderer::new);
    event.registerEntityRenderer(ModRegistry.DISQUE_TELLURIQUE.get(), DisqueTelluriqueRenderer::new);
    event.registerEntityRenderer(ModRegistry.LAME_SISMIQUE.get(), LameSismiqueRenderer::new);
    event.registerEntityRenderer(ModRegistry.PLAYER_ILLUSION.get(), PlayerIllusionRenderer::new);
    event.registerEntityRenderer(CatalogMobRegistry.CATALOG_MOB.get(), CatalogMobRenderer::new);
  }

  /** Couche d'armure d'eau — joueurs (modèle humanoïde) et entités vivantes (mobs, animaux). */
  public static void registerPlayerLayers(EntityRenderersEvent.AddLayers event) {
    for (PlayerSkin.Model skin : event.getSkins()) {
      if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
        boolean modeleSlim = skin == PlayerSkin.Model.SLIM;
        renderer.addLayer(new ArmureEauLayer(renderer, event.getContext(), modeleSlim));
        renderer.addLayer(new ArmureSangLayer(renderer, event.getContext(), modeleSlim));
        renderer.addLayer(new ArmureTerreLayer(renderer, event.getContext(), modeleSlim));
        renderer.addLayer(new GanteletsTerreLayer(renderer, event.getContext(), modeleSlim));
        renderer.addLayer(new GolemArmorLayer(renderer, event.getContext()));
      }
    }

    for (EntityType<?> entityType : event.getEntityTypes()) {
      if (event.getRenderer(entityType) instanceof LivingEntityRenderer<?, ?> livingRenderer) {
        if (livingRenderer instanceof PlayerRenderer) {
          continue;
        }
        addLivingWaterLayer(livingRenderer);
      }
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void addLivingWaterLayer(LivingEntityRenderer renderer) {
    renderer.addLayer(new ArmureEauLivingLayer(renderer));
    renderer.addLayer(new ArmureSangLivingLayer(renderer));
    renderer.addLayer(new ArmureTerreLivingLayer(renderer));
  }

  public static void registerMenuScreens(RegisterMenuScreensEvent event) {
    event.register(ModRegistry.CHEVAL_SADDLEBAG_MENU.get(), ChevalSaddlebagScreen::new);
    event.register(ModRegistry.ANE_PACK_MENU.get(), AnePackScreen::new);
    event.register(ModRegistry.MULE_PACK_MENU.get(), MulePackScreen::new);
    event.register(ModRegistry.LAMA_PACK_MENU.get(), LamaPackScreen::new);
    event.register(ModRegistry.NAVIRE_MENU.get(), NavireScreen::new);
  }

  /** Enregistre le renderer 3D item (obligatoire NeoForge 1.21+). */
  public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
    Minecraft client = Minecraft.getInstance();
    EssenceEauItemRenderer renderer = new EssenceEauItemRenderer(
        client.getBlockEntityRenderDispatcher(),
        client.getEntityModels()
    );
    EssenceEauClientExtensions.INSTANCE.bindRenderer(renderer);
    event.registerItem(EssenceEauClientExtensions.INSTANCE, ModRegistry.ESSENCE_EAU.get());

    EssenceLaveItemRenderer rendererLave = new EssenceLaveItemRenderer(
        client.getBlockEntityRenderDispatcher(),
        client.getEntityModels()
    );
    EssenceLaveClientExtensions.INSTANCE.bindRenderer(rendererLave);
    event.registerItem(EssenceLaveClientExtensions.INSTANCE, ModRegistry.ESSENCE_LAVE.get());

    EssenceGivrerItemRenderer rendererGivrer = new EssenceGivrerItemRenderer(
        client.getBlockEntityRenderDispatcher(),
        client.getEntityModels()
    );
    EssenceGivrerClientExtensions.INSTANCE.bindRenderer(rendererGivrer);
    event.registerItem(EssenceGivrerClientExtensions.INSTANCE, ModRegistry.ESSENCE_GIVRER.get());
    event.registerItem(EssenceGivrerClientExtensions.INSTANCE, ModRegistry.ESSENCE_GLACE.get());
  }
}
