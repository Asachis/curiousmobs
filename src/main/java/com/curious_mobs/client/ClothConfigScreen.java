package com.curious_mobs.client;

import com.curious_mobs.config.CuriousMobsConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 基于 Cloth Config API 的配置界面，读取/写入 Forge 的 ForgeConfigSpec。
 * 界面文案全部走语言文件，提供中文（zh_cn）与英文（en_us）两套翻译。
 */
public final class ClothConfigScreen {

  private ClothConfigScreen() {
  }

  public static Screen create(Screen parent) {
    ConfigBuilder builder = ConfigBuilder.create()
        .setParentScreen(parent)
        .setTitle(Component.translatable("curious_mobs.config.title"));
    builder.setSavingRunnable(CuriousMobsConfig.SPEC::save);

    ConfigEntryBuilder entryBuilder = builder.entryBuilder();
    ConfigCategory general = builder.getOrCreateCategory(
        Component.translatable("curious_mobs.config.category.general"));

    general.addEntry(entryBuilder.startBooleanToggle(
            Component.translatable("curious_mobs.config.transfer_curio_items"),
            CuriousMobsConfig.TRANSFER_CURIO_ITEMS.get())
        .setDefaultValue(false)
        .setSaveConsumer(CuriousMobsConfig.TRANSFER_CURIO_ITEMS::set)
        .setTooltip(Component.translatable("curious_mobs.config.transfer_curio_items.tooltip"))
        .build());

    return builder.build();
  }
}
