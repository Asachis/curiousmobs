package com.curious_mobs.util;

import java.lang.reflect.Method;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/**
 * 诡厄巫法（Goety）的纯反射兼容层：万物皆饰模组不直接依赖该模组编译，
 * 运行时通过反射调用其已确认存在的静态方法（goety-2.5.52.1 已用 javap 核验）：
 * <ul>
 *   <li>{@code SEHelper.isAlly(Player, LivingEntity)} → boolean</li>
 *   <li>{@code SEHelper.addAllyEntity(Player, LivingEntity)} → boolean</li>
 * </ul>
 * 模组未加载或反射失败时全部回退为“不可用”，保持原版行为，零副作用。
 */
public final class GoetyCompat {

  private static final String GOETY_MOD_ID = "goety";
  private static final String SE_HELPER_CLASS = "com.Polarice3.Goety.utils.SEHelper";

  private static boolean checked = false;
  private static boolean available = false;
  private static Method isAlly;
  private static Method addAllyEntity;

  private GoetyCompat() {
  }

  /** 诡厄巫法是否已确认加载且反射解析成功。 */
  public static boolean isAvailable() {
    resolve();
    return available;
  }

  private static void resolve() {
    if (checked) {
      return;
    }
    checked = true;
    if (ModList.get() == null || !ModList.get().isLoaded(GOETY_MOD_ID)) {
      return;
    }
    try {
      Class<?> helper = Class.forName(SE_HELPER_CLASS);
      isAlly = helper.getMethod("isAlly", Player.class, LivingEntity.class);
      addAllyEntity = helper.getMethod("addAllyEntity", Player.class, LivingEntity.class);
      available = true;
    } catch (ReflectiveOperationException | LinkageError ignored) {
      available = false;
    }
  }

  /** 该生物是否已登记为玩家的诡厄巫法盟友（仆从名单）。 */
  public static boolean isAlly(Player owner, LivingEntity entity) {
    resolve();
    if (!available || owner == null || entity == null) {
      return false;
    }
    try {
      return (boolean) isAlly.invoke(null, owner, entity);
    } catch (ReflectiveOperationException | LinkageError e) {
      return false;
    }
  }

  /** 将生物登记为玩家的诡厄巫法盟友（持久保存于玩家能力数据，不换实体、不留寿命）。 */
  public static boolean addAllyEntity(Player owner, LivingEntity entity) {
    resolve();
    if (!available || owner == null || entity == null) {
      return false;
    }
    try {
      return (boolean) addAllyEntity.invoke(null, owner, entity);
    } catch (ReflectiveOperationException | LinkageError e) {
      return false;
    }
  }
}