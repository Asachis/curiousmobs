package com.curious_mobs.entity;

import com.curious_mobs.registration.ModEntityTypes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

public final class ModAttributes {

  private ModAttributes() {
  }

  public static void onAttributeCreation(EntityAttributeCreationEvent event) {
    event.put(ModEntityTypes.SCARECROW.get(), ScarecrowEntity.createAttributes().build());
  }
}