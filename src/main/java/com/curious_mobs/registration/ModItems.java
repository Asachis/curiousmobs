package com.curious_mobs.registration;

import com.curious_mobs.CuriousMobs;
import com.curious_mobs.item.DevilContractItem;
import com.curious_mobs.item.RequestAndDemandItem;
import com.curious_mobs.item.SleightOfHandItem;
import com.curious_mobs.item.SubstituteScarecrowItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, CuriousMobs.MODID);

  public static final RegistryObject<Item> SLEIGHT_OF_HAND =
      ITEMS.register("sleight_of_hand", () -> new SleightOfHandItem(commonProps().durability(24)));

  public static final RegistryObject<Item> SUBSTITUTE_SCARECROW =
      ITEMS.register("substitute_scarecrow", () -> new SubstituteScarecrowItem(commonProps()));

  public static final RegistryObject<Item> REQUEST_AND_DEMAND =
      ITEMS.register("request_and_demand",
          () -> new RequestAndDemandItem(commonProps().durability(2)));

  /** 魔鬼契约仅在同装诡厄巫法（goety）与万物皆驯（mob_controller）时存在。 */
  public static final boolean DEVIL_CONTRACT_COMPAT = isLoaded("goety")
      && isLoaded("mob_controller");

  public static final RegistryObject<Item> DEVIL_CONTRACT =
      DEVIL_CONTRACT_COMPAT
          ? ITEMS.register("devil_contract",
              () -> new DevilContractItem(commonProps().durability(32)))
          : null;

  private static boolean isLoaded(String modid) {
    return ModList.get() != null && ModList.get().isLoaded(modid);
  }

  private static Item.Properties commonProps() {
    return new Item.Properties();
  }

  private ModItems() {
  }
}