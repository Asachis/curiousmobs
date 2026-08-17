package com.curious_mobs.registration;

import com.curious_mobs.CuriousMobs;
import com.curious_mobs.entity.ScarecrowEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntityTypes {

  public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
      DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CuriousMobs.MODID);

  public static final RegistryObject<EntityType<ScarecrowEntity>> SCARECROW =
      ENTITY_TYPES.register("scarecrow", () -> EntityType.Builder
          .of(ScarecrowEntity::new, MobCategory.MISC)
          .sized(0.6F, 1.95F)
          .clientTrackingRange(10)
          .setShouldReceiveVelocityUpdates(false)
          .build(CuriousMobs.MODID + ":scarecrow"));

  private ModEntityTypes() {
  }
}