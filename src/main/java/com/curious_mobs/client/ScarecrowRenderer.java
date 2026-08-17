package com.curious_mobs.client;

import com.curious_mobs.CuriousMobs;
import com.curious_mobs.entity.ScarecrowEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class ScarecrowRenderer extends
    LivingEntityRenderer<ScarecrowEntity, ScarecrowModel<ScarecrowEntity>> {

  private static final ResourceLocation TEXTURE =
      ResourceLocation.fromNamespaceAndPath(
          CuriousMobs.MODID, "textures/entity/scarecrow.png");

  public ScarecrowRenderer(EntityRendererProvider.Context context) {
    super(context, new ScarecrowModel<>(context.bakeLayer(ScarecrowModel.LAYER_LOCATION)), 0.5F);
  }

  @Override
  public ResourceLocation getTextureLocation(ScarecrowEntity entity) {
    return TEXTURE;
  }
}
