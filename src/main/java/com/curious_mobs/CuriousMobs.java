package com.curious_mobs;

import com.curious_mobs.client.ClientEvents;
import com.curious_mobs.config.CuriousMobsConfig;
import com.curious_mobs.curse.CurseEvents;
import com.curious_mobs.entity.ModAttributes;
import com.curious_mobs.registration.ModCreativeTabs;
import com.curious_mobs.registration.ModEntityTypes;
import com.curious_mobs.registration.ModItems;
import com.curious_mobs.registration.ModMenuTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CuriousMobs.MODID)
public class CuriousMobs {

  public static final String MODID = "curious_mobs";
  public static final Logger LOGGER = LoggerFactory.getLogger(CuriousMobs.class);

  /** persistentData 中暂存 ForgeCaps 的键：收容时写入、放出后经 join 事件恢复并清除。 */
  public static final String STASHED_CAPS_KEY = "curious_mobs:stashed_caps";

  @SuppressWarnings("removal")
  public CuriousMobs() {
    final net.minecraftforge.eventbus.api.IEventBus modEventBus =
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();

    ModItems.ITEMS.register(modEventBus);
    ModEntityTypes.ENTITY_TYPES.register(modEventBus);
    ModMenuTypes.MENUS.register(modEventBus);
    ModCreativeTabs.TABS.register(modEventBus);
    modEventBus.addListener(ModAttributes::onAttributeCreation);
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
        CuriousMobsConfig.SPEC);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(CurseEvents.class);

    if (FMLEnvironment.dist == Dist.CLIENT) {
      ClientEvents.init(modEventBus);
    }
  }
}