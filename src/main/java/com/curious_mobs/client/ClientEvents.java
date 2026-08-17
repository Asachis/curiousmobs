package com.curious_mobs.client;

import com.curious_mobs.entity.ScarecrowEntity;
import com.curious_mobs.registration.ModEntityTypes;
import com.curious_mobs.registration.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.client.render.CuriosLayer;

public final class ClientEvents {

  private ClientEvents() {
  }

  public static void init(IEventBus modEventBus) {
    modEventBus.addListener(ClientEvents::clientSetup);
    modEventBus.addListener(ClientEvents::registerRenderers);
    modEventBus.addListener(ClientEvents::registerLayerDefinitions);
    modEventBus.addListener(ClientEvents::addLayers);
  }

  @SuppressWarnings("removal")
  public static void clientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(
        () -> MenuScreens.register(ModMenuTypes.MOB_CURIOS.get(), MobCuriosScreen::new));
    registerConfigScreenFactory();
  }

  /**
   * 在模组列表注册配置按钮（Cloth Config API 提供的界面）。
   * Cloth Config 为可选依赖：运行时未安装时跳过注册，不影响模组本体加载。
   */
  @SuppressWarnings("removal")
  private static void registerConfigScreenFactory() {
    try {
      Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
      net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
          net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
          () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
              ClothConfigScreen::create));
    } catch (ClassNotFoundException ignored) {
    }
  }

  public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(ModEntityTypes.SCARECROW.get(), ScarecrowRenderer::new);
  }

  public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
    event.registerLayerDefinition(ScarecrowModel.LAYER_LOCATION, ScarecrowModel::createBodyLayer);
  }

  public static void addLayers(EntityRenderersEvent.AddLayers event) {
    for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
      if (type == EntityType.PLAYER) {
        continue;
      }
      if (!LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
        continue;
      }
      @SuppressWarnings("unchecked")
      net.minecraft.client.renderer.entity.EntityRenderer<?> renderer =
          event.getRenderer((EntityType<? extends LivingEntity>) type);

      if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
        addCuriosLayer(livingRenderer);
      }
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void addCuriosLayer(LivingEntityRenderer livingRenderer) {
    livingRenderer.addLayer(new CuriosLayer<>(livingRenderer));
  }
}