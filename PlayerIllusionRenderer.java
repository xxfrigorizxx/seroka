package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seroka.entity.PlayerIllusionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class PlayerIllusionRenderer extends LivingEntityRenderer<PlayerIllusionEntity, PlayerModel<PlayerIllusionEntity>> {

  public PlayerIllusionRenderer(EntityRendererProvider.Context context) {
    super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    this.addLayer(
        new HumanoidArmorLayer<>(
            this,
            new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()
        )
    );
    this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
  }

  @Override
  public void render(
      PlayerIllusionEntity entity,
      float entityYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    this.setModelProperties(entity);
    super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(PlayerIllusionEntity entity) {
    return resolveSkin(entity.getOwnerUuid()).texture();
  }

  private void setModelProperties(PlayerIllusionEntity entity) {
    PlayerModel<PlayerIllusionEntity> model = getModel();
    model.setAllVisible(true);
    model.crouching = entity.isCrouching();

    if (!(resolveOwner(entity.getOwnerUuid()) instanceof AbstractClientPlayer clientPlayer)) {
      return;
    }

    model.hat.visible = clientPlayer.isModelPartShown(PlayerModelPart.HAT);
    model.jacket.visible = clientPlayer.isModelPartShown(PlayerModelPart.JACKET);
    model.leftPants.visible = clientPlayer.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
    model.rightPants.visible = clientPlayer.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
    model.leftSleeve.visible = clientPlayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
    model.rightSleeve.visible = clientPlayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);

    HumanoidModel.ArmPose mainPose = getArmPose(clientPlayer, InteractionHand.MAIN_HAND);
    HumanoidModel.ArmPose offPose = getArmPose(clientPlayer, InteractionHand.OFF_HAND);
    if (mainPose.isTwoHanded()) {
      offPose = clientPlayer.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
    }

    if (clientPlayer.getMainArm() == HumanoidArm.RIGHT) {
      model.rightArmPose = mainPose;
      model.leftArmPose = offPose;
    } else {
      model.rightArmPose = offPose;
      model.leftArmPose = mainPose;
    }
  }

  private static HumanoidModel.ArmPose getArmPose(AbstractClientPlayer player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (stack.isEmpty()) {
      return HumanoidModel.ArmPose.EMPTY;
    }
    if (player.getUsedItemHand() == hand && player.getUseItemRemainingTicks() > 0) {
      UseAnim useAnim = stack.getUseAnimation();
      if (useAnim == UseAnim.BLOCK) {
        return HumanoidModel.ArmPose.BLOCK;
      }
      if (useAnim == UseAnim.BOW) {
        return HumanoidModel.ArmPose.BOW_AND_ARROW;
      }
      if (useAnim == UseAnim.SPEAR) {
        return HumanoidModel.ArmPose.THROW_SPEAR;
      }
      if (useAnim == UseAnim.CROSSBOW && hand == player.getUsedItemHand()) {
        return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
      }
      if (useAnim == UseAnim.SPYGLASS) {
        return HumanoidModel.ArmPose.SPYGLASS;
      }
      if (useAnim == UseAnim.TOOT_HORN) {
        return HumanoidModel.ArmPose.TOOT_HORN;
      }
      if (useAnim == UseAnim.BRUSH) {
        return HumanoidModel.ArmPose.BRUSH;
      }
    } else if (!player.swinging && stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
      return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }
    return HumanoidModel.ArmPose.ITEM;
  }

  private static Player resolveOwner(UUID ownerUuid) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return null;
    }
    Player player = minecraft.level.getPlayerByUUID(ownerUuid);
    if (player != null) {
      return player;
    }
    return minecraft.player != null && minecraft.player.getUUID().equals(ownerUuid) ? minecraft.player : null;
  }

  private static PlayerSkin resolveSkin(UUID ownerUuid) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player instanceof AbstractClientPlayer clientPlayer && clientPlayer.getUUID().equals(ownerUuid)) {
      return clientPlayer.getSkin();
    }
    if (minecraft.getConnection() != null) {
      var info = minecraft.getConnection().getPlayerInfo(ownerUuid);
      if (info != null) {
        return info.getSkin();
      }
    }
    return DefaultPlayerSkin.get(ownerUuid);
  }
}
