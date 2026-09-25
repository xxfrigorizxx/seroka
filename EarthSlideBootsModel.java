package com.seroka.client.model;

import com.seroka.ModMain;
import com.seroka.entity.EarthSlideEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Vague tellurique de 5 blocs de large. Le monticule est centré sous le joueur (il surfe dedans),
 * la crête déferlante se dresse et s'enroule vers l'avant derrière lui.
 */
public class EarthSlideBootsModel extends EntityModel<EarthSlideEntity> {

  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(ModMain.id("earth_slide"), "main");

  private static final int SHARD_COUNT = 5;
  private static final float[] SHARD_X = {-26.0F, -12.0F, 0.5F, 13.0F, 25.0F};
  private static final float[] SHARD_Y = {14.0F, 19.0F, 24.0F, 20.0F, 15.0F};
  private static final float[] SHARD_Z = {-34.0F, -36.0F, -38.0F, -36.0F, -34.0F};

  private final ModelPart board;
  private final ModelPart mound;
  private final ModelPart crest;
  private final ModelPart[] shards = new ModelPart[SHARD_COUNT];

  private final float boardBaseY;
  private final float moundBaseY;
  private final float crestBaseY;

  private float roll;
  private float pitch;
  private boolean moving;

  public EarthSlideBootsModel(ModelPart root) {
    this.board = root.getChild("board");
    this.mound = root.getChild("mound");
    this.crest = root.getChild("crest");
    for (int i = 0; i < SHARD_COUNT; i++) {
      this.shards[i] = root.getChild("shard_" + i);
    }
    this.boardBaseY = board.y;
    this.moundBaseY = mound.y;
    this.crestBaseY = crest.y;
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition meshdefinition = new MeshDefinition();
    PartDefinition partdefinition = meshdefinition.getRoot();

    // Socle de roche entre les pieds et le sol : le joueur survole de 0.45 bloc (~7 unités).
    partdefinition.addOrReplaceChild(
        "board",
        CubeListBuilder.create()
            .texOffs(0, 190)
            .addBox(-6.0F, -10.0F, -8.0F, 12.0F, 11.0F, 17.0F)
            .texOffs(60, 190)
            .addBox(-4.0F, -9.0F, 9.0F, 8.0F, 9.0F, 4.0F)
            .texOffs(90, 190)
            .addBox(-2.5F, -8.0F, 13.0F, 5.0F, 7.0F, 3.0F),
        PartPose.offset(0.0F, 0.5F, 0.0F)
    );

    // Flancs en gradins : 80 unités de large (5 blocs), creux central où se tient le joueur.
    // La base descend sous le niveau du sol, la terre masque la partie enterrée.
    partdefinition.addOrReplaceChild(
        "mound",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-18.0F, -14.0F, -30.0F, 10.0F, 19.0F, 48.0F)
            .texOffs(0, 60)
            .addBox(-28.0F, -14.0F, -28.0F, 10.0F, 15.0F, 44.0F)
            .texOffs(0, 112)
            .addBox(-40.0F, -14.0F, -26.0F, 12.0F, 11.0F, 38.0F)
            .texOffs(120, 0)
            .addBox(8.0F, -14.0F, -30.0F, 10.0F, 19.0F, 48.0F)
            .texOffs(120, 60)
            .addBox(18.0F, -14.0F, -28.0F, 10.0F, 15.0F, 44.0F)
            .texOffs(120, 112)
            .addBox(28.0F, -14.0F, -26.0F, 12.0F, 11.0F, 38.0F)
            .texOffs(60, 160)
            .addBox(-8.0F, -14.0F, 14.0F, 16.0F, 13.0F, 10.0F),
        PartPose.offset(0.0F, 0.0F, 0.0F)
    );

    // Mur déferlant derrière, avec lèvres qui s'enroulent vers l'avant au-dessus du joueur.
    partdefinition.addOrReplaceChild(
        "crest",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-11.0F, -14.0F, -10.0F, 22.0F, 30.0F, 10.0F)
            .texOffs(0, 44)
            .addBox(-11.0F, 12.0F, 0.0F, 22.0F, 6.0F, 9.0F)
            .texOffs(66, 0)
            .addBox(-23.0F, -14.0F, -9.0F, 12.0F, 27.0F, 9.0F)
            .texOffs(66, 40)
            .addBox(-23.0F, 9.0F, 0.0F, 12.0F, 5.0F, 7.0F)
            .texOffs(112, 0)
            .addBox(11.0F, -14.0F, -9.0F, 12.0F, 27.0F, 9.0F)
            .texOffs(112, 40)
            .addBox(11.0F, 9.0F, 0.0F, 12.0F, 5.0F, 7.0F)
            .texOffs(0, 62)
            .addBox(-33.0F, -14.0F, -8.0F, 10.0F, 22.0F, 8.0F)
            .texOffs(40, 62)
            .addBox(23.0F, -14.0F, -8.0F, 10.0F, 22.0F, 8.0F)
            .texOffs(80, 62)
            .addBox(-40.0F, -14.0F, -7.0F, 7.0F, 18.0F, 7.0F)
            .texOffs(104, 62)
            .addBox(33.0F, -14.0F, -7.0F, 7.0F, 18.0F, 7.0F),
        PartPose.offset(0.0F, 0.0F, -30.0F)
    );

    for (int i = 0; i < SHARD_COUNT; i++) {
      partdefinition.addOrReplaceChild(
          "shard_" + i,
          CubeListBuilder.create()
              .texOffs(200, 200)
              .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
          PartPose.offset(SHARD_X[i], SHARD_Y[i], SHARD_Z[i])
      );
    }

    return LayerDefinition.create(meshdefinition, 256, 256);
  }

  /** Inclinaisons transmises par le renderer (virage et vitesse). */
  public void setSurfPose(float roll, float pitch, boolean moving) {
    this.roll = roll;
    this.pitch = pitch;
    this.moving = moving;
  }

  @Override
  public void setupAnim(
      EarthSlideEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
  ) {
    float bob = Mth.abs(Mth.sin(limbSwing * 0.6662F) * limbSwingAmount) * 0.8F;

    board.y = boardBaseY + bob * 0.5F;
    board.zRot = roll;
    board.xRot = pitch;

    mound.y = moundBaseY + bob * 0.4F;
    mound.zRot = roll * 0.4F;
    mound.xRot = pitch * 0.5F;

    crest.visible = moving;
    crest.y = crestBaseY + bob + Mth.sin(ageInTicks * 0.4F) * 0.6F;
    crest.xRot = pitch - 0.1F + Mth.sin(ageInTicks * 0.3F) * 0.04F;
    crest.zRot = roll * 0.6F + Mth.sin(ageInTicks * 0.22F) * 0.04F;

    for (int i = 0; i < SHARD_COUNT; i++) {
      ModelPart shard = shards[i];
      shard.visible = moving;
      float phase = ageInTicks * (0.25F + i * 0.05F) + i * 1.7F;
      shard.xRot = phase;
      shard.yRot = phase * 0.7F;
      shard.zRot = phase * 0.5F;
      shard.y = SHARD_Y[i] + Mth.sin(phase) * 1.8F;
      shard.z = SHARD_Z[i] + Mth.abs(Mth.sin(phase * 0.5F)) * 3.0F;
    }
  }

  @Override
  public void renderToBuffer(
      com.mojang.blaze3d.vertex.PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
      int packedLight,
      int packedOverlay,
      int color
  ) {
    mound.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    crest.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    board.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    for (ModelPart shard : shards) {
      shard.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
  }
}
