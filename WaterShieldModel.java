package com.seroka.client.model;

import com.seroka.ModMain;
import com.seroka.entity.WaterShieldEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Mur d'eau voxel en 3 panneaux — forme en C (flancs courbés vers le joueur). */
public class WaterShieldModel extends HierarchicalModel<WaterShieldEntity> {

  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(ModMain.id("water_shield"), "main");

  private static final float WALL_DEPTH = 4.0F;
  private static final float HALF_WIDTH = 16.0F;
  private static final float HALF_HEIGHT = 16.0F;
  private static final float FLANK_WIDTH = 16.0F;
  /** Flancs à 135° — combiné au flip 180° du renderer pour former le C. */
  private static final float FLANK_YAW_DEG = 135.0F;

  private final ModelPart root;

  public WaterShieldModel(ModelPart root) {
    this.root = root;
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition rootDef = mesh.getRoot();

    rootDef.addOrReplaceChild(
        "center",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-HALF_WIDTH, -HALF_HEIGHT, 0.0F, HALF_WIDTH * 2.0F, HALF_HEIGHT * 2.0F, WALL_DEPTH, CubeDeformation.NONE),
        PartPose.ZERO
    );

    rootDef.addOrReplaceChild(
        "left_flank",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(0.0F, -HALF_HEIGHT, 0.0F, FLANK_WIDTH, HALF_HEIGHT * 2.0F, WALL_DEPTH, CubeDeformation.NONE),
        PartPose.offsetAndRotation(
            -HALF_WIDTH, 0.0F, 0.0F,
            0.0F, (float) Math.toRadians(FLANK_YAW_DEG), 0.0F
        )
    );

    rootDef.addOrReplaceChild(
        "right_flank",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-FLANK_WIDTH, -HALF_HEIGHT, 0.0F, FLANK_WIDTH, HALF_HEIGHT * 2.0F, WALL_DEPTH, CubeDeformation.NONE),
        PartPose.offsetAndRotation(
            HALF_WIDTH, 0.0F, 0.0F,
            0.0F, (float) Math.toRadians(-FLANK_YAW_DEG), 0.0F
        )
    );

    return LayerDefinition.create(mesh, 64, 64);
  }

  @Override
  public void setupAnim(
      WaterShieldEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
  ) {
  }

  @Override
  public ModelPart root() {
    return root;
  }
}
