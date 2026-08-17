package com.curious_mobs.registration;

import com.curious_mobs.CuriousMobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {

  public static final DeferredRegister<CreativeModeTab> TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CuriousMobs.MODID);

  public static final RegistryObject<CreativeModeTab> CURSED_TAB = TABS.register("curious_mobs",
      () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup.curious_mobs"))
          .icon(() -> new ItemStack(ModItems.REQUEST_AND_DEMAND.get()))
          .displayItems((params, output) -> {
            output.accept(ModItems.SLEIGHT_OF_HAND.get());
            output.accept(ModItems.SUBSTITUTE_SCARECROW.get());
            output.accept(ModItems.REQUEST_AND_DEMAND.get());
            if (ModItems.DEVIL_CONTRACT != null) {
              output.accept(ModItems.DEVIL_CONTRACT.get());
            }
          })
          .build());

  private ModCreativeTabs() {
  }
}