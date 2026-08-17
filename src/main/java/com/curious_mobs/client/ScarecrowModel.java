package com.curious_mobs.client;

import com.curious_mobs.CuriousMobs;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
public class ScarecrowModel<T extends Entity> extends EntityModel<T> {

  // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into
  // this model's constructor
  public static final ModelLayerLocation LAYER_LOCATION =
      new ModelLayerLocation(
          ResourceLocation.fromNamespaceAndPath(CuriousMobs.MODID, "scarecrow"), "main");

  private final ModelPart bone;
  private final ModelPart bb_main;

  public ScarecrowModel(ModelPart root) {
    this.bone = root.getChild("bone");
    this.bb_main = root.getChild("bb_main");
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition meshdefinition = new MeshDefinition();
    PartDefinition partdefinition = meshdefinition.getRoot();

    PartDefinition bone =
        partdefinition.addOrReplaceChild(
            "bone",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F))
                .texOffs(0, 38)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.3054F, 0.0F, 0.0F));

    PartDefinition cube_r1 =
        bone.addOrReplaceChild(
            "cube_r1",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(32, 12)
                .addBox(0.0F, -2.0F, 0.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, 0.0F, 0.9599F, 0.0F));

    PartDefinition bb_main =
        partdefinition.addOrReplaceChild(
            "bb_main",
            CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-4.5F, -20.0F, -2.5F, 9.0F, 14.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(36, 0)
                .addBox(-4.5F, -11.0F, -2.5F, 9.0F, 0.0F, 5.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 24.0F, 0.0F));

    PartDefinition cube_r2 =
        bb_main.addOrReplaceChild(
            "cube_r2",
            CubeListBuilder.create()
                .texOffs(46, 9)
                .addBox(-0.5F, 4.5F, -2.0F, 5.0F, 0.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(28, 31)
                .addBox(-0.5F, -0.5F, -2.0F, 5.0F, 11.0F, 4.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(3.5F, -19.5F, 0.0F, 0.0F, 0.0F, -0.1309F));

    PartDefinition cube_r3 =
        bb_main.addOrReplaceChild(
            "cube_r3",
            CubeListBuilder.create()
                .texOffs(46, 9)
                .addBox(-4.0F, 3.0F, -2.0F, 5.0F, 0.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(28, 16)
                .addBox(-4.0F, -1.0F, -2.0F, 5.0F, 11.0F, 4.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(-4.0F, -19.0F, 0.0F, 0.0F, 0.0F, 0.1309F));

    PartDefinition cube_r4 =
        bb_main.addOrReplaceChild(
            "cube_r4",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-0.5F, -5.5F, -0.5F, 1.0F, 11.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, -5.5F, 0.25F, 0.0F, 0.4363F, 0.0F));

    return LayerDefinition.create(meshdefinition, 64, 64);
  }

  @Override
  public void setupAnim(
      T entity,
      float limbSwing,
      float limbSwingAmount,
      float ageInTicks,
      float netHeadYaw,
      float headPitch) {}

  @Override
  public void renderToBuffer(
      com.mojang.blaze3d.vertex.PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
      int packedLight,
      int packedOverlay,
      float red,
      float green,
      float blue,
      float alpha) {
    bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    bb_main.render(
        poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
  }
}