package com.curious_mobs.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CuriousMobsConfig {

  public static final ForgeConfigSpec SPEC;

  /**
   * 请求与强求转移诅咒时，是否把诅咒饰品本体也转移到目标生物身上。
   * 默认 false：饰品保留在玩家身上，仅永久移除诅咒效果（并压制 EL 的被动诅咒逻辑），
   * 保留饰品提供的正面增益。置 true 时效果与替死稻草人一致（物品本身被移走）。
   */
  public static final ForgeConfigSpec.BooleanValue TRANSFER_CURIO_ITEMS;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

    builder.comment(
        "万物皆饰模组配置（Curious Mobs configuration）",
        "请求与强求：转移诅咒时是否把诅咒饰品本体也移至目标生物。",
        "false（默认）：饰品保留在玩家身上，只移除诅咒效果并保留饰品增益。",
        "true：诅咒饰品本体被转移到目标生物身上（饰品本身被移走）。")
        .push("request_and_demand");

    TRANSFER_CURIO_ITEMS = builder
        .comment(
            "是否将诅咒饰品本体转移至目标生物。",
            "默认 false：永久清空玩家自身的诅咒效果（含 EL 被动诅咒），但保留饰品在玩家身上的增益。",
            "如果设为 true，诅咒饰品会被放到目标生物的饰品栏（同替死稻草人）。")
        .translation("curious_mobs.config.transfer_curio_items")
        .define("transferCurioItems", false);

    builder.pop();

    SPEC = builder.build();
  }

  private CuriousMobsConfig() {
  }
}