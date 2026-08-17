package com.curious_mobs.mixin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.common.data.CuriosEntityManager;

@Mixin(CuriosEntityManager.class)
public class CuriosEntityManagerMixin {

  @Shadow(remap = false)
  private Map<EntityType<?>, Map<String, ISlotType>> entitySlots;

  @Inject(method = "getEntitySlots", at = @At("HEAD"), cancellable = true, remap = false)
  private void curious_mobs$fallbackToPlayerSlots(EntityType<?> type,
      CallbackInfoReturnable<Map<String, ISlotType>> cir) {
    if (type == EntityType.PLAYER) {
      return;
    }
    if (this.entitySlots.containsKey(type)) {
      return;
    }
    Map<String, ISlotType> playerSlots = this.entitySlots.get(EntityType.PLAYER);
    if (playerSlots != null && !playerSlots.isEmpty()) {
      cir.setReturnValue(
          Collections.unmodifiableMap(new LinkedHashMap<>(playerSlots)));
    }
  }
}