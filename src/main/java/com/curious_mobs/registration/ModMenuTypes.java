package com.curious_mobs.registration;

import com.curious_mobs.CuriousMobs;
import com.curious_mobs.menu.MobCuriosMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {

  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(ForgeRegistries.MENU_TYPES, CuriousMobs.MODID);

  public static final RegistryObject<MenuType<MobCuriosMenu>> MOB_CURIOS =
      MENUS.register("mob_curios",
          () -> IForgeMenuType.create(MobCuriosMenu::fromNetwork));

  private ModMenuTypes() {
  }
}